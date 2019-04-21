package services
import model.identity.{GuestRegistrationRequest, GuestRegistrationResponse, User, UserResponse}
import org.mockito.{Matchers => Match}
import org.mockito.Mockito._
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatest.mockito.MockitoSugar
import utils.MockWSHelper
import cats.implicits._
import cats.data.EitherT
import exceptions.DeserializationException
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IdentityServiceSpec extends WordSpecLike with MockWSHelper with MockitoSugar with Matchers with ScalaFutures {

  "IdentityServiceSpec" should {

    "getOrCreateIdentity and get a user id" in {
      val mockClient = mock[IdentityClient]

      val identityService = new IdentityService(mockClient)

      when(mockClient.getUser(Match.any()))
        .thenReturn(EitherT.pure[Future, Exception](UserResponse(User(100L))))

      identityService.getOrCreateIdentity("myemail@example.com").value.futureValue.right.get shouldBe 100L

    }

    "getOrCreateIdentity and create a user id" in {
      val mockClient = mock[IdentityClient]

      val identityService = new IdentityService(mockClient)

      when(mockClient.getUser(Match.any()))
        .thenReturn(EitherT.leftT[Future, UserResponse] {
          val e: Exception = DeserializationException("nothing here to see")
          e
        })
      when(mockClient.createAccount(Match.any()))
        .thenReturn(EitherT.pure[Future, Exception](GuestRegistrationResponse(GuestRegistrationRequest(200L))))

      identityService.getOrCreateIdentity("myemail@example.com")
        .value.futureValue.right.get shouldBe 200L
    }

    "getOrCreateIdentity and fail to create a user id" in {
      val mockClient = mock[IdentityClient]

      val identityService = new IdentityService(mockClient)

      when(mockClient.getUser(Match.any()))
        .thenReturn(EitherT.leftT[Future, UserResponse] {
          val e: Exception = DeserializationException("nothing here to see")
          e
        })
      when(mockClient.createAccount(Match.any()))
        .thenReturn(EitherT.leftT[Future, GuestRegistrationResponse]{
          val e: Exception = DeserializationException("nothing here to see")
          e
        }
        )

      identityService.getOrCreateIdentity("myemail@example.com")
        .value.futureValue.left.get.getMessage shouldBe "nothing here to see"
    }

  }
}
