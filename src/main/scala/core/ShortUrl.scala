package core

import akka.actor.ActorSystem
import com.redis.RedisClient
import spray.http.StatusCodes
import spray.routing.{Route, SimpleRoutingApp, ValidationRejection}

import scala.concurrent.Future
import scala.util.Random

/**
 *
 */
object ShortUrl extends App with SimpleRoutingApp {

  implicit val actorSystem = ActorSystem("Short-Url")
  val redisClient = new RedisClient()
  startServer("0.0.0.0", 8080)(route)


  implicit def executionContext = actorRefFactory.dispatcher

  def route = Route {
    path("shorten") {
      post { ctx =>
        val r = ctx.request.entity.asString
        val url = Random.alphanumeric.take(7).mkString
        val f = Future(redisClient.set(url, r))

        f.mapTo[Boolean].map {
          case true => ctx.complete {
            s"http://localhost:8080/$url"
          }
          case _ => ctx.reject(ValidationRejection(s"Cannot save url $r"))
        }
      }
    }~
    path(Rest) { r =>
      get { ctx =>
        Future(redisClient.get(r)).mapTo[Option[String]].map {
          case Some(result) => ctx.redirect(result, StatusCodes.MovedPermanently)
          case None => ctx.reject(ValidationRejection(s"Unknown short url $r"))
        }
      }
    }

    //}
  }
}
