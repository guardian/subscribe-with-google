package model

import play.api.libs.json._


//todo: Improve naming
sealed trait IncomingGoogleEvent {
  val version: String
  val packageName: String
  val eventTimeMillis: Long
}


case class IncomingSubscriptionEvent(version: String,
                                 packageName: String,
                                 eventTimeMillis: Long,
                                 subscriptionNotification: SubscriptionNotification
                                ) extends IncomingGoogleEvent


case class IncomingTestEvent(version: String,
                             packageName: String,
                             eventTimeMillis: Long,
                             testNotification: TestNotification
                            ) extends IncomingGoogleEvent


object IncomingGoogleEvent {

  implicit val formatSubscriptionEvent = Json.format[IncomingSubscriptionEvent]
  implicit val formatTestEvent = Json.format[IncomingTestEvent]

  implicit val writes: Writes[IncomingGoogleEvent] = new Writes[IncomingGoogleEvent] {
    override def writes(o: IncomingGoogleEvent): JsValue = {
      o match {
        case sub: IncomingSubscriptionEvent => Json.toJson[IncomingSubscriptionEvent](sub)
        case test: IncomingTestEvent => Json.toJson[IncomingTestEvent](test)
      }
    }
  }

  implicit val reads: Reads[IncomingGoogleEvent] = new Reads[IncomingGoogleEvent] {
    override def reads(json: JsValue): JsResult[IncomingGoogleEvent] = {
      ((json \ "subscriptionNotification").toOption, (json \ "testNotification").toOption) match {
        case (Some(_), None) => json.validate[IncomingSubscriptionEvent]
        case (None, Some(_)) => json.validate[IncomingTestEvent]
        case (Some(s), Some(t)) =>
          JsError(s"This event contains both subscription and test data - ${s.toString()} :: ${t.toString()}")
        case _ => JsError("Does not contain any notification types")
      }
    }
  }

}