package routing.adt
import model.{SubscriptionDeveloperNotification, SubscriptionPurchase}

sealed trait ContributionWithSubscriptionPurchase {
  val subscriptionDeveloperNotification: SubscriptionDeveloperNotification
  val subscriptionPurchase: SubscriptionPurchase
}

case class SingleContributionWithSubscriptionPurchase(
    subscriptionDeveloperNotification: SubscriptionDeveloperNotification,
    subscriptionPurchase: SubscriptionPurchase)
    extends ContributionWithSubscriptionPurchase
case class RecurringContributionWithSubscriptionPurchase(
    subscriptionDeveloperNotification: SubscriptionDeveloperNotification,
    subscriptionPurchase: SubscriptionPurchase)
    extends ContributionWithSubscriptionPurchase

object ContributionWithSubscriptionPurchase {

  def apply[routing](contribution: Contribution,
            subscriptionPurchase: SubscriptionPurchase): ContributionWithSubscriptionPurchase = {
    contribution match {
      case c: SingleContribution =>
        SingleContributionWithSubscriptionPurchase(c.subscriptionDeveloperNotification, subscriptionPurchase)
      case c: RecurringContribution =>
        RecurringContributionWithSubscriptionPurchase(c.subscriptionDeveloperNotification, subscriptionPurchase)
    }
  }
}
