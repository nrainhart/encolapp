package com.example

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.scaladsl.SourceQueueWithComplete

case class Alumno(nombre: String)
case class Aula(
  participantes: List[Alumno] = List.empty,
  interesados: List[Alumno] = List.empty
) {
  def agregarParticipante(alumno: Alumno): Aula = copy(participantes = (participantes ++ List(alumno)).distinct)//TODO fallar si el nombre ya existe
  def quitarParticipante(alumno: Alumno): Aula = copy(participantes = participantes.filterNot(_ == alumno))
  def agregarInteresado(alumno: Alumno): Aula = copy(interesados = (interesados ++ List(alumno)).distinct)//TODO validar que esté como participante
  def quitarInteresado(alumno: Alumno): Aula = copy(interesados = interesados.filterNot(_ == alumno))//TODO validar que esté como participante
}

sealed trait EventoEntrada
sealed trait EventoSalida
final case class Entro(alumno: Alumno) extends EventoEntrada with EventoSalida
final case class Salio(alumno: Alumno) extends EventoEntrada with EventoSalida
final case class QuiereHablar(alumno: Alumno) extends EventoEntrada with EventoSalida
final case class YaNoQuiereHablar(alumno: Alumno) extends EventoEntrada with EventoSalida
final case class EstadoActual(replyTo: ActorRef[Aula]) extends EventoEntrada

class AulaActor(private val publicadorDeEventos: SourceQueueWithComplete[EventoSalida]) {//TODO renombrar a AulaActorCreator?
  def actor(): Behavior[EventoEntrada] = registry(Aula())

  private def registry(aula: Aula): Behavior[EventoEntrada] =
    Behaviors.receiveMessage { eventoEntrada =>
      def publicarEvento(eventoSalida: EventoSalida) = publicadorDeEventos.offer(eventoSalida)

      eventoEntrada match {
        case evento @ Entro(alumno) =>
          publicarEvento(evento)
          registry(aula.agregarParticipante(alumno))
        case evento @ Salio(alumno) =>
          publicarEvento(evento)
          registry(aula.quitarParticipante(alumno))
        case evento @ QuiereHablar(alumno) =>
          publicarEvento(evento)
          registry(aula.agregarInteresado(alumno))
        case evento @ YaNoQuiereHablar(alumno) =>
          publicarEvento(evento)
          registry(aula.quitarInteresado(alumno))
        case EstadoActual(replyTo) =>
          replyTo ! aula
          Behaviors.same
      }
    }
}
