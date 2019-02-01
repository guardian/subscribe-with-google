package utils

object FlattenableEither {

  implicit class FlattenableEither[+A, B](either: Either[A, Either[A, B]]) {
    def flatten: Either[A, B] = {
      either match {
        case Left(l) => Left(l)
        case Right(Left(l)) => Left(l)
        case Right(Right(r)) => Right(r)
      }
    }
  }
}
