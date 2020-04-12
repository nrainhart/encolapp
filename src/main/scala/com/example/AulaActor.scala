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

  def estaAnotadoComoInteresado(alumno: Alumno): Boolean = interesados.contains(alumno)
}

case class AulaVersionada(aula: Aula, version: Int)

sealed trait EventoEntrada
sealed trait EventoSalida
final case class Entro(alumno: Alumno) extends EventoEntrada
final case class Salio(alumno: Alumno) extends EventoEntrada
final case class QuiereHablar(alumno: Alumno) extends EventoEntrada with EventoSalida
final case class YaNoQuiereHablar(alumno: Alumno) extends EventoEntrada with EventoSalida
final case class EstadoActual(replyTo: ActorRef[AulaVersionada]) extends EventoEntrada

case class EventoVersionado(evento: EventoSalida, version: Int)

class AulaActor(private val publicadorDeEventos: SourceQueueWithComplete[EventoVersionado]) {
  def behavior(): Behavior[EventoEntrada] = registry(AulaVersionada(Aula(), 0))

  private def registry(aulaVersionada: AulaVersionada): Behavior[EventoEntrada] =
    Behaviors.receiveMessage { eventoEntrada =>
      val aula = aulaVersionada.aula
      val versionActualizada = aulaVersionada.version + 1
      def publicarEvento(eventoVersionado: EventoVersionado) = publicadorDeEventos.offer(eventoVersionado)

      eventoEntrada match {
        case Entro(alumno) =>
          registry(AulaVersionada(aula.agregarParticipante(alumno), versionActualizada))
        case Salio(alumno) =>
          registry(AulaVersionada(aula.quitarParticipante(alumno), versionActualizada))
        case evento @ QuiereHablar(alumno) =>
          if (!aula.estaAnotadoComoInteresado(alumno)) {
            publicarEvento(EventoVersionado(evento, versionActualizada))
          }
          registry(AulaVersionada(aula.agregarInteresado(alumno), versionActualizada))
        case evento @ YaNoQuiereHablar(alumno) =>
          if (aula.estaAnotadoComoInteresado(alumno)) {
            publicarEvento(EventoVersionado(evento, versionActualizada))
          }
          registry(AulaVersionada(aula.quitarInteresado(alumno), versionActualizada))
        case EstadoActual(replyTo) =>
          replyTo ! aulaVersionada
          Behaviors.same
      }
    }
}
