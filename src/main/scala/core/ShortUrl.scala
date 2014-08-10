package core

import java.net.URLEncoder
import java.nio.charset.{StandardCharsets, Charset}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.actor.ActorSystem
import com.redis.RedisClient
import spray.http.StatusCodes
import spray.httpx.marshalling.Marshaller
import spray.routing.{Route, SimpleRoutingApp, ValidationRejection}

import scala.concurrent.Future

/**
 *
 */
object ShortUrl extends App with SimpleRoutingApp {

  implicit val actorSystem = ActorSystem("Short-Url")
  val algo = "HmacSHA1"
  val mac = Mac.getInstance(algo)
  val redisClient = new RedisClient()
  mac.init(new SecretKeySpec("short-url".getBytes(), algo))
  startServer("0.0.0.0", 8080)(route)


  implicit def executionContext = actorRefFactory.dispatcher

  def route = Route {
    path("shorten") {
      post { ctx =>
        val r = ctx.request.entity.asString
        val url = URLEncoder.encode(mac.doFinal(r.getBytes()).mkString, StandardCharsets.UTF_8.toString)
        println(url)
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
