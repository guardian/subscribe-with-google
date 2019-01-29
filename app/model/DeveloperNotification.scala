package model

import play.api.libs.json._

sealed trait DeveloperNotification {
  val version: String
  val packageName: String
  val eventTimeMillis: Long
}


case class SubscriptionDeveloperNotification(version: String,
                                             packageName: String,
                                             eventTimeMillis: Long,
                                             subscriptionNotification: SubscriptionNotification
                                ) extends DeveloperNotification


case class TestDeveloperNotification(version: String,
                                     packageName: String,
                                     eventTimeMillis: Long,
                                     testNotification: TestNotification
                            ) extends DeveloperNotification


object DeveloperNotification {

  implicit val formatSubscriptionEvent = Json.format[SubscriptionDeveloperNotification]
  implicit val formatTestEvent = Json.format[TestDeveloperNotification]

  implicit val writes: Writes[DeveloperNotification] = new Writes[DeveloperNotification] {
    override def writes(o: DeveloperNotification): JsValue = {
      o match {
        case sub: SubscriptionDeveloperNotification => Json.toJson[SubscriptionDeveloperNotification](sub)
        case test: TestDeveloperNotification => Json.toJson[TestDeveloperNotification](test)
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