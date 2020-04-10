package com.example

import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat}
import spray.json._

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val alumnoJsonFormat = jsonFormat1(Alumno)
  implicit val aulaJsonFormat = jsonFormat2(Aula)
  implicit val entroJsonFormat = jsonFormat1(Entro)
  implicit val salioJsonFormat = jsonFormat1(Salio)
  implicit val quiereHablarJsonFormat = jsonFormat1(QuiereHablar)
  implicit val yaNoQuiereHablarJsonFormat = jsonFormat1(YaNoQuiereHablar)

  implicit val eventoSalidaFormat = new RootJsonFormat[EventoSalida] {
    def write(obj: EventoSalida): JsValue =
      JsObject((obj match {
        case e: Entro => e.toJson
        case s: Salio => s.toJson
        case s: QuiereHablar => s.toJson
        case s: YaNoQuiereHablar => s.toJson
      }).asJsObject.fields  + ("type" -> JsString(obj.getClass.getSimpleName)))

    def read(json: JsValue): EventoSalida =
      json.asJsObject.getFields("type") match {
        case Seq(JsString("Entro")) => json.convertTo[Entro]
        case Seq(JsString("Salio")) => json.convertTo[Salio]
        case Seq(JsString("QuiereHablar")) => json.convertTo[QuiereHablar]
        case Seq(JsString("YaNoQuiereHablar")) => json.convertTo[YaNoQuiereHablar]
      }
  }

  implicit val aulaVersionadaFormat = jsonFormat2(AulaVersionada)
  implicit val eventoVersionadoFormat = jsonFormat2(EventoVersionado)
}
