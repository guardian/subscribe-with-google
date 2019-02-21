package model

import org.scalatest.{Matchers, WordSpecLike}

class NotificationTypeSpec extends WordSpecLike with Matchers {

  "NotificationType enum" must {
    "have 9 types" in {
      NotificationType.values.size shouldBe 11
    }
    "be creatable from Int" in {
      NotificationType.withValue(1) shouldBe NotificationType.SubscriptionRecovered
      NotificationType.withValue(2) shouldBe NotificationType.SubscriptionRenewed
      NotificationType.withValue(3) shouldBe NotificationType.SubscriptionCanceled
      NotificationType.withValue(4) shouldBe NotificationType.SubscriptionPurchased
      NotificationType.withValue(5) shouldBe NotificationType.SubscriptionOnHold
      NotificationType.withValue(6) shouldBe NotificationType.SubscriptionInGracePeriod
      NotificationType.withValue(7) shouldBe NotificationType.SubscriptionRestarted
      NotificationType.withValue(8) shouldBe NotificationType.SubscriptionPriceChangeConfirmed
      NotificationType.withValue(9) shouldBe NotificationType.SubscriptionDeferred
      NotificationType.withValue(12) shouldBe NotificationType.SubscriptionRevoked
      NotificationType.withValue(13) shouldBe NotificationType.SubscriptionExpired
    }
  }
}


