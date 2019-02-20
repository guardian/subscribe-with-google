package model

import play.api.libs.json._
import play.api.libs.functional.syntax._

sealed trait DeveloperNotification {
  val version: String
  val packageName: String
  val eventTimeMillis: Long
}

case class SubscriptionDeveloperNotification(version: String,
                                             packageName: String,
                                             eventTimeMillis: Long,
                                             subscriptionNotification: SubscriptionNotification)
    extends DeveloperNotification

case class TestDeveloperNotification(version: String,
                                     packageName: String,
                                     eventTimeMillis: Long,
                                     testNotification: TestNotification)
    extends DeveloperNotification

object DeveloperNotification {

  implicit val readSubscriptionEvent: Reads[SubscriptionDeveloperNotification] = (json: JsValue) => {
    ((__ \ "version").read[String] and
      (__ \ "packageName").read[String] and
      (__ \ "eventTimeMillis").read[String].map[Long](_.toLong) and
      (__ \ "subscriptionNotification").read[SubscriptionNotification])(SubscriptionDeveloperNotification.apply _)
  }.reads(json)

  implicit val readTestSubscriptionEvent: Reads[TestDeveloperNotification] = (json: JsValue) => {
    ((__ \ "version").read[String] and
      (__ \ "packageName").read[String] and
      (__ \ "eventTimeMillis").read[String].map[Long](_.toLong) and
      (__ \ "testNotification").read[TestNotification])(TestDeveloperNotification.apply _)
  }.reads(json)

  implicit val writeSubscriptionEvent: Writes[SubscriptionDeveloperNotification] =
    new Writes[SubscriptionDeveloperNotification] {
      override def writes(o: SubscriptionDeveloperNotification): JsValue = {
        Json.obj(
          "version" -> o.version,
          "packageName" -> o.packageName,
          "eventTimeMillis" -> o.eventTimeMillis.toString,
          "subscriptionNotification" -> Json.toJson(o.subscriptionNotification)
        )
      }
    }

  implicit val writeTestEvent: Writes[TestDeveloperNotification] = (o: TestDeveloperNotification) => {
    Json.obj("version" -> o.version,
             "packageName" -> o.packageName,
             "eventTimeMillis" -> o.eventTimeMillis.toString,
             "testNotification" -> Json.toJson(o.testNotification))
  }

  implicit val writes: Writes[DeveloperNotification] = new Writes[DeveloperNotification] {
    override def writes(o: DeveloperNotification): JsValue = {
      o match {
        case sub: SubscriptionDeveloperNotification => Json.toJson[SubscriptionDeveloperNotification](sub)
        case test: TestDeveloperNotification        => Json.toJson[TestDeveloperNotification](test)
      }
    }
  }

  implicit val reads: Reads[DeveloperNotification] = new Reads[DeveloperNotification] {
    override def reads(json: JsValue): JsResult[DeveloperNotification] = {
      ((json \ "subscriptionNotification").toOption, (json \ "testNotification").toOption) match {
        case (Some(_), None) => json.validate[SubscriptionDeveloperNotification]
        case (None, Some(_)) => json.validate[TestDeveloperNotification]
        case (Some(s), Some(t)) =>
          JsError(s"This event contains both subscription and test data - ${s.toString()} :: ${t.toString()}")
        case _ => JsError("Does not contain any notification types")
      }
    }
  }

}
