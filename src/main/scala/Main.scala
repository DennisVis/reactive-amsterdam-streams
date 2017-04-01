import java.nio.file.Paths

import akka.{ Done, NotUsed }
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.ws.{ Message, TextMessage, WebSocketRequest }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ FileIO, Flow, Sink, Source }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Failure, Success }

object Main extends App {
  val filePath = Paths.get(ClassLoader.getSystemResource("tzu-quotes.txt").toURI)

  // Needed for streams to leverage actors for their functionality
  implicit val system = ActorSystem("MyActorSystem")
  implicit val materializer = ActorMaterializer()



  // A perfectly useless stream
  val stream1 = Source.maybe.to(Sink.ignore)
  //stream1.run()



  // A stream of a single element to standard output
  val stream2 = Source.single("Hello World!").to(Sink.foreach(println))
  //stream2.run()



  // A stream of a repeating element to standard output
  val stream3 = Source.repeat("Hello World!").to(Sink.foreach(println))
  //stream3.run()



  // A stream of a repeating element to standard output using tick
  val stream4 = Source.tick(0 seconds, 2 seconds, "Hello World!").to(Sink.foreach(println))
  //stream4.run()



  // A stream of a repeating element to standard output using tick with a modified flow
  val flow5 = Flow[String].map(_.length)
  val stream5 = Source
    .tick(0 seconds, 2 seconds, "Hello World!")
    .via(flow5)
    .to(Sink.foreach(println))
  //stream5.run()



  // A stream of a repeating element to standard output using tick with a modified flow
  val stream6 = Source
    .tick(0 seconds, 2 seconds, "Hello World!")
    .map(_.length) // The flow
    .to(Sink.foreach(println))
  //stream6.run()



  // A stream of a file delimited by end of line to standard output
  val source7 = FileIO
    .fromPath(filePath)
    .map(_.utf8String)
    .flatMapConcat { fileAsString =>
      Source(fileAsString.split("\n").toList)
    }
    .map { line =>
      Thread.sleep(2000)
      line
    }
  val stream7 = source7.to(Sink.foreach(println))
  //stream7.run()



  // A stream of a text message sent and received over WebSocket
  val source8 = Source.tick(0 seconds, 2 seconds, TextMessage.Strict("Hello World!"))
  val sink8: Sink[Message, Future[Done]] = Sink.foreach {
    case TextMessage.Strict(msg) => println(msg)
    case _ =>
  }
  val flow8 = Flow.fromSinkAndSource(sink8, source8)
  val stream8 = Http()
//    .singleWebSocketRequest(WebSocketRequest("ws://echo.websocket.org"), flow8)



  // A stream of a file delimited by end of line sent over WebSocket
  val source9 = source7.map(TextMessage.Strict.apply)
  val flow9 = Flow.fromSinkAndSource(Sink.ignore, source9)
  val stream9 = Http()
//    .bindAndHandle(handleWebSocketMessages(flow9), "localhost", 8888)



  // A stream receiving WebSocket messages and sending them to the standard output
  val sink10: Sink[Message, Future[Done]] = Sink.foreach {
    case TextMessage.Strict(msg) => println(msg)
//    case streamed: TextMessage.Streamed => streamed.textStream.to(Sink.foreach(println)).run()
//    case streamed: TextMessage.Streamed =>
//      streamed.textStream.fold("")(_ ++ _).to(Sink.foreach(println)).run()
    case _ =>
  }
  val flow10 = Flow.fromSinkAndSource(sink10, Source.maybe)
  val stream10 = Http()
    .bindAndHandle(handleWebSocketMessages(flow10), "localhost", 8888).map { _ =>
      val source10a = Source.single(TextMessage.Streamed(Source(List("Hello", " ", "World", "!"))))
      val flow10a = Flow.fromSinkAndSource(Sink.ignore, source10a)
      Http().singleWebSocketRequest(WebSocketRequest("ws://localhost:8888"), flow10a)
    }
}
