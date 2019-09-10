package arrow.fx.mtl

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.Tuple2
import arrow.core.getOrElse
import arrow.mtl.StateT
import arrow.mtl.StateTOf
import arrow.mtl.StateTPartialOf
import arrow.mtl.extensions.StateTMonadThrow
import arrow.mtl.fix
import arrow.fx.Ref
import arrow.fx.typeclasses.Async
import arrow.fx.typeclasses.Bracket
import arrow.fx.typeclasses.ExitCase
import arrow.fx.typeclasses.MonadDefer
import arrow.fx.typeclasses.Proc
import arrow.fx.typeclasses.ProcF
import arrow.extension
import arrow.mtl.runM
import arrow.typeclasses.MonadError
import arrow.undocumented
import kotlin.coroutines.CoroutineContext

@extension
@undocumented
interface StateTBracket<F, S> : Bracket<StateTPartialOf<F, S>, Throwable>, StateTMonadThrow<F, S> {

  fun MD(): MonadDefer<F>

  override fun ME(): MonadError<F, Throwable> = MD()

  override fun <A, B> StateTOf<F, S, A>.bracketCase(
    release: (A, ExitCase<Throwable>) -> StateTOf<F, S, Unit>,
    use: (A) -> StateTOf<F, S, B>
  ): StateT<F, S, B> = MD().run {

    StateT.liftF<F, S, Ref<F, Option<S>>>(this, Ref(this, None)).flatMap { ref ->
      StateT<F, S, B>(this) { startS ->
        runM(this, startS).bracketCase(use = { (s, a) ->
          use(a).runM(this, s).flatMap { sa ->
            ref.set(Some(sa.a)).map { sa }
          }
        }, release = { (s0, a), exitCase ->
          when (exitCase) {
            is ExitCase.Completed ->
              ref.get().map { it.getOrElse { s0 } }.flatMap { s1 ->
                release(a, ExitCase.Completed).fix().runS(this, s1).flatMap { s2 ->
                  ref.set(Some(s2))
                }
              }
            else -> release(a, exitCase).runM(this, s0).unit()
          }
        }).flatMap { (s, b) -> ref.get().map { it.getOrElse { s } }.tupleRight(b) }
      }
    }
  }
}

@extension
@undocumented
interface StateTMonadDefer<F, S> : MonadDefer<StateTPartialOf<F, S>>, StateTBracket<F, S> {

  override fun MD(): MonadDefer<F>

  override fun <A> defer(fa: () -> StateTOf<F, S, A>): StateT<F, S, A> = MD().run {
    StateT(this) { s -> defer { fa().runM(this, s) } }
  }
}

@extension
@undocumented
interface StateTAsyncInstane<F, S> : Async<StateTPartialOf<F, S>>, StateTMonadDefer<F, S> {

  fun AS(): Async<F>

  override fun MD(): MonadDefer<F> = AS()

  override fun <A> async(fa: Proc<A>): StateT<F, S, A> = AS().run {
    StateT.liftF(this, async(fa))
  }

  override fun <A> asyncF(k: ProcF<StateTPartialOf<F, S>, A>): StateT<F, S, A> = AS().run {
    StateT.invoke(this) { s ->
      asyncF<A> { cb -> k(cb).fix().runA(this, s) }
        .map { Tuple2(s, it) }
    }
  }

  override fun <A> StateTOf<F, S, A>.continueOn(ctx: CoroutineContext): StateT<F, S, A> = AS().run {
    StateT(this) { s -> runM(this, s).continueOn(ctx) }
  }
}
