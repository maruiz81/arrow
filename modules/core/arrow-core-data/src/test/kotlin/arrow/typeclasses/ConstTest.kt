package arrow.typeclasses

import arrow.core.extensions.const.applicative.applicative
import arrow.core.extensions.const.eq.eq
import arrow.core.extensions.const.show.show
import arrow.core.extensions.monoid
import arrow.core.extensions.const.traverseFilter.traverseFilter
import arrow.test.UnitSpec
import arrow.test.laws.ApplicativeLaws
import arrow.test.laws.EqLaws
import arrow.test.laws.ShowLaws
import arrow.test.laws.TraverseFilterLaws
import io.kotlintest.runner.junit4.KotlinTestRunner
import org.junit.runner.RunWith

@RunWith(KotlinTestRunner::class)
class ConstTest : UnitSpec() {
  init {
    Int.monoid().run {
      testLaws(
        TraverseFilterLaws.laws(Const.traverseFilter(), Const.applicative(this), { Const(it) }, Eq.any()),
        ApplicativeLaws.laws(Const.applicative(this), Eq.any()),
        EqLaws.laws(Const.eq<Int, Int>(Eq.any())) { Const(it) },
        ShowLaws.laws(Const.show(), Const.eq<Int, Int>(Eq.any())) { Const(it) }
      )
    }
  }
}
