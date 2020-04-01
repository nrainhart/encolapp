package com.example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.SourceQueueWithComplete

case class Alumno(nombre: String)
case class Aula(
  participantes: List[Alumno] = List.empty,
  interesados: List[Alumno] = List.empty
) {
  def agregarParticipante(alumno: Alumno): Aula = copy(participantes = (participantes ++ List(alumno)).distinct)
  def quitarParticipante(alumno: Alumno): Aula = copy(participantes = participantes.filterNot(_ == alumno))
  def agregarInteresado(alumno: Alumno): Aula = copy(interesados = (interesados ++ List(alumno)).distinct)//TODO validar que esté como participante
  def quitarInteresado(alumno: Alumno): Aula = copy(interesados = interesados.filterNot(_ == alumno))//TODO validar que esté como participante
}

sealed trait Evento
final case class Entro(alumno: Alumno) extends Evento
final case class Salio(alumno: Alumno) extends Evento
final case class QuiereHablar(alumno: Alumno) extends Evento
final case class YaNoQuiereHablar(alumno: Alumno) extends Evento
final case class EstadoActual(replyTo: ActorRef[Aula]) extends Evento

class AulaActor(private val publicadorDeEventos: SourceQueueWithComplete[Evento]) {//TODO renombrar a AulaActorCreator?
  def actor(): Behavior[Evento] = registry(Aula())

  private def registry(aula: Aula): Behavior[Evento] =
    Behaviors.receiveMessage { evento =>
      publicadorDeEventos.offer(evento)
      evento match {
        case Entro(alumno) =>
          registry(aula.agregarParticipante(alumno))
        case Salio(alumno) =>
          registry(aula.quitarParticipante(alumno))
        case QuiereHablar(alumno) =>
          registry(aula.agregarInteresado(alumno))
        case YaNoQuiereHablar(alumno) =>
          registry(aula.quitarInteresado(alumno))
        case EstadoActual(replyTo) =>
          replyTo ! aula
          Behaviors.same
      }
    }
}
