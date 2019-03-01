package routing
import model.{SubscriptionNotification, SubscriptionPurchase}

sealed trait SubscriptionTypes {
  val subscriptionNotification: SubscriptionNotification
  val subscriptionPurchase: Option[SubscriptionPurchase]
}

case class SingleContribution(subscriptionNotification: SubscriptionNotification,
                              subscriptionPurchase: Option[SubscriptionPurchase])
    extends SubscriptionTypes
//todo: Better naming
case class SingleContributionWithoutEmail(subscriptionNotification: SubscriptionNotification,
                                          subscriptionPurchase: Option[SubscriptionPurchase])
    extends SubscriptionTypes
case class RecurringContribution(subscriptionNotification: SubscriptionNotification,
                                 subscriptionPurchase: Option[SubscriptionPurchase])
    extends SubscriptionTypes

//todo: Better naming
case class RecurringContributionWithoutEmail(subscriptionNotification: SubscriptionNotification,
                                             subscriptionPurchase: Option[SubscriptionPurchase])
    extends SubscriptionTypes

case class AndroidInAppPurchase(subscriptionNotification: SubscriptionNotification,
                                subscriptionPurchase: Option[SubscriptionPurchase])
    extends SubscriptionTypes
