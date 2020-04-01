package com.example

import spray.json.{DefaultJsonProtocol, JsValue, RootJsonFormat}

import scala.collection.mutable

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val alumnoJsonFormat = jsonFormat1(Alumno)
  implicit val aulaJsonFormat = jsonFormat2(Aula)
}
