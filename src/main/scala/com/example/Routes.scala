package com.example

import akka.NotUsed
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import akka.util.Timeout

import scala.collection.mutable
import scala.util.{Failure, Success}

class Routes(context: ActorContext[Nothing]) {

  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  private implicit val system: ActorSystem[_] = context.system
  // If ask takes more time than this to complete the request is failed
  private implicit val timeout: Timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  val aulas: mutable.Map[String, (ActorRef[Evento], Flow[Message, Message, Any])] = mutable.Map.empty

  def createAulaActor(): (ActorRef[Evento], Flow[Message, Message, Any]) = {
    val (sourceQueueEventos, sourceEventos): (SourceQueueWithComplete[Evento], Source[Evento, NotUsed]) =
      Source.queue[Evento](Integer.MAX_VALUE, OverflowStrategy.dropTail)
        .preMaterialize()

    val aulaActor: ActorRef[Evento] = context.spawn(new AulaActor(sourceQueueEventos).actor(), "AulaActor")
    context.watch(aulaActor)

    val serializarEvento: Evento => TextMessage = evento => TextMessage(evento.toString)
    val wsSource: Source[TextMessage, NotUsed] = sourceEventos.map {serializarEvento}
    wsSource.runWith(Sink.ignore) // Necessary to prevent the stream from closing?
    val wsHandler: Flow[Message, Message, Any] = Flow.fromSinkAndSource(Sink.ignore, wsSource)
    (aulaActor, wsHandler)
  }

  aulas.put("iasc", createAulaActor())

  def aulaActorPara(nombreAula: String): (ActorRef[Evento], Flow[Message, Message, Any]) =
    aulas.getOrElse(nombreAula, throw new RuntimeException(s"No se pudo encontrar el aula $nombreAula"))//TODO debería devolver 404

  val routes: Route =
    pathPrefix("aula" / Segment) { nombreAula =>
      val (aulaActor, wsHandler) = aulaActorPara(nombreAula)
      concat(
        path("eventos") {
          handleWebSocketMessages(wsHandler)
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
            onComplete (aulaActor.ask(EstadoActual)) {
              case Success(aula) => complete(aula)
              case Failure(exception) => complete(exception)
            }
          }
        }
      )
    }
}
