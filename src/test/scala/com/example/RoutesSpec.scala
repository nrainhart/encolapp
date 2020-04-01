package com.example

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

/*
class RoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest {

  // the Akka HTTP route testkit does not yet support a typed actor system (https://github.com/akka/akka-http/issues/2036)
  // so we have to adapt for now
  lazy val testKit = ActorTestKit()
  implicit def typedSystem = testKit.system
  override def createActorSystem(): akka.actor.ActorSystem = testKit.system.toClassic

  // Here we need to implement all the abstract members of UserRoutes.
  // We use the real UserRegistryActor to test it while we hit the Routes,
  // but we could "mock" it by implementing it in-place or by using a TestProbe
  // created with testKit.createTestProbe()
  val aulaMockeada = testKit.spawn(new AulaActor(Source.empty[Evento]).actor)//TODO no compila
  lazy val routes = new Routes(aulaMockeada).routes

  // use the json formats to marshal and unmarshall objects in the test
  import JsonFormats._
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

  "Routes" should {
    "be able to add users (POST /users)" in {
      val alumno = Alumno("Nico")
      val userEntity = Marshal(alumno).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      // using the RequestBuilding DSL:
      val request = Post("/users").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.Created)

        // we expect the response to be json:
        contentType should ===(ContentTypes.`application/json`)

        // and we know what message we're expecting back:
        entityAs[String] should ===("""{"description":"User Kapi created."}""")
      }
    }
  }
}
*/
