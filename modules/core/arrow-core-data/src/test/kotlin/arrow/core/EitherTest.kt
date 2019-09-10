package arrow.core

import arrow.Kind
import arrow.Kind2
import arrow.core.extensions.combine
import arrow.core.extensions.eq
import arrow.core.extensions.hash
import arrow.core.extensions.monoid
import arrow.core.extensions.semigroup
import arrow.core.extensions.either.applicative.applicative
import arrow.core.extensions.either.applicativeError.handleErrorWith
import arrow.core.extensions.either.bifunctor.bifunctor
import arrow.core.extensions.either.bitraverse.bitraverse
import arrow.core.extensions.either.eq.eq
import arrow.core.extensions.either.hash.hash
import arrow.core.extensions.either.monadError.monadError
import arrow.core.extensions.either.monoid.monoid
import arrow.core.extensions.either.semigroup.semigroup
import arrow.core.extensions.either.semigroupK.semigroupK
import arrow.core.extensions.either.show.show
import arrow.core.extensions.either.traverse.traverse
import arrow.test.UnitSpec
import arrow.test.generators.either
import arrow.test.generators.intSmall
import arrow.test.laws.BifunctorLaws
import arrow.test.laws.HashLaws
import arrow.test.laws.MonadErrorLaws
import arrow.test.laws.MonoidLaws
import arrow.test.laws.SemigroupKLaws
import arrow.test.laws.SemigroupLaws
import arrow.test.laws.ShowLaws
import arrow.test.laws.TraverseLaws
import arrow.test.laws.BitraverseLaws
import arrow.typeclasses.Eq
import arrow.typeclasses.Hash
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.runner.junit4.KotlinTestRunner
import org.junit.runner.RunWith

@RunWith(KotlinTestRunner::class)
class EitherTest : UnitSpec() {
  val EQ: Eq<Kind<EitherPartialOf<ForId>, Int>> = Eq { a, b ->
    a.fix() == b.fix()
  }

  val EQ2: Eq<Kind2<ForEither, Int, Int>> = Eq { a, b ->
    a.fix() == b.fix()
  }

  init {

    testLaws(
      BifunctorLaws.laws(Either.bifunctor(), { Right(it) }, EQ2),
      SemigroupLaws.laws(Either.semigroup(String.semigroup(), String.semigroup()), Either.right("1"), Either.right("2"), Either.right("3"), Either.eq(String.eq(), String.eq())),
      MonoidLaws.laws(Either.monoid(MOL = String.monoid(), MOR = Int.monoid()), Gen.either(Gen.string(), Gen.int()), Either.eq(String.eq(), Int.eq())),
      ShowLaws.laws(Either.show(), Either.eq(String.eq(), Int.eq())) { Right(it) },
      MonadErrorLaws.laws(Either.monadError(), Eq.any(), Eq.any()),
      TraverseLaws.laws(Either.traverse(), Either.applicative(), { Right(it) }, Eq.any()),
      BitraverseLaws.laws(Either.bitraverse(), { Right(it) }, Eq.any()),
      SemigroupKLaws.laws(Either.semigroupK(), Either.applicative(), EQ),
      HashLaws.laws(Either.hash(Hash.any(), Int.hash()), EQ2) { Right(it) }
    )

    "empty should return a Right of the empty of the inner type" {
      forAll { _: String ->
        Right(String.monoid().run { empty() }) == Either.monoid(String.monoid(), String.monoid()).run { empty() }
      }
    }

    "combine two rights should return a right of the combine of the inners" {
      forAll { a: String, b: String ->
        String.monoid().run { Either.right(a.combine(b)) } == Either.right(a).combine(String.monoid(), String.monoid(), Either.right(b))
      }
    }

    "combine two lefts should return a left of the combine of the inners" {
      forAll { a: String, b: String ->
        String.monoid().run { Either.left(a.combine(b)) } == Either.left(a).combine(String.monoid(), String.monoid(), Either.left(b))
      }
    }

    "combine a right and a left should return left" {
      forAll { a: String, b: String ->
        Either.left(a) == Either.left(a).combine(String.monoid(), String.monoid(), Either.right(b)) &&
          Either.left(a) == Either.right(b).combine(String.monoid(), String.monoid(), Either.left(a))
      }
    }

    "getOrElse should return value" {
      forAll { a: Int, b: Int ->
        Right(a).getOrElse { b } == a &&
          Left(a).getOrElse { b } == b
      }
    }

    "orNull should return value" {
      forAll { a: Int ->
        Either.Right(a).orNull() == a
      }
    }

    "getOrHandle should return value" {
      forAll { a: Int, b: Int ->
        Right(a).getOrHandle { b } == a &&
          Left(a).getOrHandle { it + b } == a + b
      }
    }

    "filterOrElse should filter values" {
      forAll(Gen.intSmall(), Gen.intSmall()) { a: Int, b: Int ->
        val left: Either<Int, Int> = Left(a)

        Right(a).filterOrElse({ it > a - 1 }, { b }) == Right(a) &&
          Right(a).filterOrElse({ it > a + 1 }, { b }) == Left(b) &&
          left.filterOrElse({ it > a - 1 }, { b }) == Left(a) &&
          left.filterOrElse({ it > a + 1 }, { b }) == Left(a)
      }
    }

    "filterOrOther should filter values" {
      forAll(Gen.intSmall(), Gen.intSmall()) { a: Int, b: Int ->
        val left: Either<Int, Int> = Left(a)

        Right(a).filterOrOther({ it > a - 1 }, { b + a }) == Right(a) &&
          Right(a).filterOrOther({ it > a + 1 }, { b + a }) == Left(b + a) &&
          left.filterOrOther({ it > a - 1 }, { b + a }) == Left(a) &&
          left.filterOrOther({ it > a + 1 }, { b + a }) == Left(a)
      }
    }

    "leftIfNull should return Left if Right value is null of if Either is Left" {
      forAll { a: Int, b: Int ->
        Right(a).leftIfNull { b } == Right(a) &&
          Right(null).leftIfNull { b } == Left(b) &&
          Left(a).leftIfNull { b } == Left(a)
      }
    }

    "rightIfNotNull should return Left if value is null or Right of value when not null" {
      forAll { a: Int, b: Int ->
        null.rightIfNotNull { b } == Left(b) &&
          a.rightIfNotNull { b } == Right(a)
      }
    }

    "swap should interchange values" {
      forAll { a: Int ->
        Left(a).swap() == Right(a) &&
          Right(a).swap() == Left(a)
      }
    }

    "toOption should convert" {
      forAll { a: Int ->
        Right(a).toOption() == Some(a) &&
          Left(a).toOption() == None
      }
    }

    "contains should check value" {
      forAll(Gen.intSmall(), Gen.intSmall()) { a: Int, b: Int ->
        Right(a).contains(a) &&
          !Right(a).contains(b) &&
          !Left(a).contains(a)
      }
    }

    "mapLeft should alter left instance only" {
      forAll(Gen.intSmall(), Gen.intSmall()) { a: Int, b: Int ->
        val right: Either<Int, Int> = Right(a)
        val left: Either<Int, Int> = Left(b)
        right.mapLeft { it + 1 } == right && left.mapLeft { it + 1 } == Left(b + 1)
      }
    }

    "cond should create right instance only if test is true" {
      forAll { t: Boolean, i: Int, s: String ->
        val expected = if (t) Right(i) else Left(s)
        Either.cond(t, { i }, { s }) == expected
      }
    }

    "handleErrorWith should handle left instance otherwise return Right" {
      forAll { a: Int, b: Int ->
        Left(a).handleErrorWith { Right(b) } == Right(b) &&
          Right(a).handleErrorWith { Right(b) } == Right(a)
      }
    }
  }
}
