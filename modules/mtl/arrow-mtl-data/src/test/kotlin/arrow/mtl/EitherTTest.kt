package arrow.mtl

import arrow.Kind
import arrow.core.Either
import arrow.core.ForId
import arrow.core.Id
import arrow.core.Left
import arrow.core.Option
import arrow.core.Right
import arrow.core.value
import arrow.core.extensions.const.divisible.divisible
import arrow.core.extensions.id.functor.functor
import arrow.fx.ForIO
import arrow.fx.IO
import arrow.fx.mtl.eithert.async.async
import arrow.fx.extensions.io.applicativeError.attempt
import arrow.fx.extensions.io.async.async
import arrow.core.extensions.id.monad.monad
import arrow.core.extensions.id.traverse.traverse
import arrow.core.extensions.monoid
import arrow.core.extensions.option.functor.functor
import arrow.mtl.extensions.eithert.applicative.applicative
import arrow.mtl.extensions.eithert.divisible.divisible
import arrow.mtl.extensions.eithert.functor.functor
import arrow.mtl.extensions.eithert.semigroupK.semigroupK
import arrow.mtl.extensions.eithert.traverse.traverse
import arrow.fx.typeclasses.seconds
import arrow.test.UnitSpec
import arrow.test.laws.AsyncLaws
import arrow.test.laws.DivisibleLaws
import arrow.test.laws.SemigroupKLaws
import arrow.test.laws.TraverseLaws
import arrow.typeclasses.Const
import arrow.typeclasses.ConstPartialOf
import arrow.typeclasses.Eq
import arrow.typeclasses.const
import arrow.typeclasses.value
import io.kotlintest.properties.forAll
import io.kotlintest.runner.junit4.KotlinTestRunner
import org.junit.runner.RunWith

@RunWith(KotlinTestRunner::class)
class EitherTTest : UnitSpec() {

  fun <A> EQ(): Eq<Kind<EitherTPartialOf<ForIO, Throwable>, A>> = Eq { a, b ->
    a.value().attempt().unsafeRunTimed(60.seconds) == b.value().attempt().unsafeRunTimed(60.seconds)
  }

  init {

    testLaws(
      DivisibleLaws.laws(
        EitherT.divisible<ConstPartialOf<Int>, Int>(Const.divisible(Int.monoid())),
        { EitherT(it.const()) },
        Eq { a, b -> a.value().value() == b.value().value() }
      ),
      AsyncLaws.laws(EitherT.async(IO.async()), EQ(), EQ()),
      TraverseLaws.laws(EitherT.traverse<ForId, Int>(Id.traverse()), EitherT.functor<ForId, Int>(Id.functor()), { EitherT(Id(Right(it))) }, Eq.any()),
      SemigroupKLaws.laws(
        EitherT.semigroupK<ForId, Int>(Id.monad()),
        EitherT.applicative<ForId, Int>(Id.monad()),
        Eq.any())
    )

    "mapLeft should alter left instance only" {
      forAll { i: Int, j: Int ->
        val left: Either<Int, Int> = Left(i)
        val right: Either<Int, Int> = Right(j)
        EitherT(Option(left)).mapLeft(Option.functor()) { it + 1 } == EitherT(Option(Left(i + 1))) &&
          EitherT(Option(right)).mapLeft(Option.functor()) { it + 1 } == EitherT(Option(right)) &&
          EitherT(Option.empty<Either<Int, Int>>()).mapLeft(Option.functor()) { it + 1 } == EitherT(Option.empty<Either<Int, Int>>())
      }
    }
  }
}
