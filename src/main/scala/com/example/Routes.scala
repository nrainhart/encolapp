package com.example

import akka.NotUsed
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import akka.util.Timeout
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.model.HttpOriginMatcher
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import concurrent.duration._

import scala.util.{Failure, Success}

class Routes(context: ActorContext[Nothing]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  // If ask takes more time than this to complete the request is failed
  private implicit val system: ActorSystem[_] = context.system
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  val (sourceQueueEventos, sourceEventos): (SourceQueueWithComplete[Evento], Source[Evento, NotUsed]) =
    Source.queue[Evento](Integer.MAX_VALUE, OverflowStrategy.dropTail)
      .preMaterialize()
  sourceEventos.runWith(Sink.ignore) // Necessary to prevent the stream from closing?

  val aulaActor: ActorRef[Evento] = context.spawn(new AulaActor(sourceQueueEventos).actor(), "AulaActor")
  context.watch(aulaActor)

  val routes: Route =
    pathPrefix("aula") {
      concat(
        path("eventos") {
          get {
            cors(CorsSettings.defaultSettings.withAllowedOrigins(HttpOriginMatcher.*)) {
              complete {
                sourceEventos
                  .map(evento => ServerSentEvent(evento.toString))
                  .keepAlive(1.second, () => ServerSentEvent.heartbeat)
              }
            }
          }
        },
        path("participantes") {
          concat(
            post {
              entity(as[Alumno]) { alumno =>
                aulaActor.tell(Entro(alumno))
                complete(StatusCodes.OK)
              }
            },
            delete {
              entity(as[Alumno]) { alumno =>
                aulaActor.tell(Salio(alumno))
                complete(StatusCodes.OK)
              }
            }
          )
        },
        path("interesados") {
          concat(
            post {
              entity(as[Alumno]) { alumno =>
                aulaActor.tell(QuiereHablar(alumno))
                complete(StatusCodes.OK)
              }
            },
            delete {
              entity(as[Alumno]) { alumno =>
                aulaActor.tell(YaNoQuiereHablar(alumno))
                complete(StatusCodes.OK)
              }
            }
          )
        },
        pathEnd {
          get {
            onComplete(aulaActor.ask(EstadoActual)) {
              case Success(aula) => complete(aula)
              case Failure(exception) => complete(exception)
            }
          }
        }
      )
    }
}
