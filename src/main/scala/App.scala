import java.security.SecureRandom
import java.time.{LocalDateTime, ZoneId}

import akka.http.scaladsl.ConnectionContext
import com.softwaremill.sttp._
import com.softwaremill.sttp.akkahttp.AkkaHttpBackend
import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}
import java.security.cert.X509Certificate

import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import io.circe._
import io.circe.optics.JsonPath.root
import io.circe.parser._

object App extends App with StrictLogging{

  var lastMessageSent: scala.collection.mutable.Set[Either[String, String]] = scala.collection.mutable.Set()

  implicit val sttpBackend = NoVerifyHttpBackend.backend

  val p = "<input id=\"p\" type=\"hidden\" value=\"(.*)\" />".r.unanchored
  val k = "<input id=\"k\" type=\"hidden\" value=\"(.*)\" />".r.unanchored

  val BASE_URL =
    uri"https://burghquayregistrationoffice.inis.gov.ie/Website/AMSREG/AMSRegWeb.nsf/AppSelect?OpenForm"

  def current_milli_time =
    LocalDateTime.now().atZone(ZoneId.of("Europe/Dublin")).getNano

  def get_apt_url(k: String, p: String) =
    uri"https://burghquayregistrationoffice.inis.gov.ie/Website/AMSREG/AMSRegWeb.nsf/(getAppsNear)?readform&cat=All&sbcat=All&typ=Renewal&k=$k&p=$p&_=$current_milli_time()"

  def getToken() = {
    val baseHtml =
      Await.result(sttp.get(BASE_URL).send().map(_.unsafeBody), Duration.Inf)

    p.findAllIn(baseHtml)
    val pParam = baseHtml match {
      case p(a) => a
      case _    => sys.error("boom")
    }
    val kParam = baseHtml match {
      case k(a) => a
      case _    => sys.error("boom")
    }
    (pParam, kParam)
  }

  while (true) {

    try {
      val tokens = getToken()
      val res = Await.result(
        sttp
          .headers(
            Map(
              ("Referer",
                "https://burghquayregistrationoffice.inis.gov.ie/Website/AMSREG/AMSRegWeb.nsf/AppSelect?OpenForm")
            ))
          .get(get_apt_url(tokens._2, tokens._1))
          .send()
          .map(_.body),
        Duration.Inf
      )

      {
        for {
          r <- res
          _ = logger.debug(r)
          j <- parse(r)
        } yield root.slots.each.time.string.getAll(j)
      } match {
        case Right(v) if v.size > 0 => {
          val messages = v.map(s => {
            val x = s.split("-")
            s"Your appointment is coming up on ${x(0)} at ${x(1)}"
          })

          messages.foreach( message => {
            if(lastMessageSent.exists( msgs => msgs match {
              case Right(v) => message.equals(v)
              case _ => false
            })) {

            } else {
              val sendResponse = Await.result(
                Twilio.sendMessageReq(message).send().map(_.body),
                Duration.Inf
              )
              logger.info(s"$sendResponse")
              lastMessageSent += sendResponse.map(_ => message)
            }
          })
        }
        case _ => ()
      }

      Thread.sleep(1000)
    } catch {
      case e: Throwable => logger.error("Exception during this run", e)
    }
  }
}

object NoVerifyHttpBackend {

  val sc: SSLContext = SSLContext.getInstance("TLS")
  sc.init(
    null,
    Array[TrustManager] {
      new X509TrustManager() {
        def getAcceptedIssuers = Array[X509Certificate]()

        def checkClientTrusted(certs: Array[X509Certificate],
                               authType: String): Unit = {}

        def checkServerTrusted(certs: Array[X509Certificate],
                               authType: String): Unit = {}
      }
    },
    new SecureRandom
  )

  val backend =
    AkkaHttpBackend(
      customHttpsContext = Some(ConnectionContext.https(sc))
    )
}

object Twilio {
  val sid = "AC52b2cf53a57bc623ac932e7bfa16c49b"
  val token = "5bf91187f864ea8d499babf3e691d8a7"

  val apiUrl =
    uri"https://api.twilio.com/2010-04-01/Accounts/AC52b2cf53a57bc623ac932e7bfa16c49b/Messages.json"

  def reqBody(body: String) = Map(
    "To"   -> "whatsapp:+353834077070",
    "From" -> "whatsapp:+14155238886",
    "Body" -> body
  )

  def reqBody1(body: String) = Map(
    "To"   -> "whatsapp:+919620151886",
    "From" -> "whatsapp:+14155238886",
    "Body" -> body
  )

  def sendMessageReq(body: String) =
    sttp
      .post(apiUrl)
      .body(reqBody(body))
      .auth
      .basic(sid, token)

  def sendMessageReq1(body: String) =
    sttp
      .post(apiUrl)
      .body(reqBody1(body))
      .auth
      .basic(sid, token)
}
