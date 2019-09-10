package arrow.fx.reactor

import arrow.core.Either
import arrow.fx.KindConnection
import arrow.fx.typeclasses.ExitCase
import arrow.fx.typeclasses.MonadDefer

typealias MonoKConnection = KindConnection<ForMonoK>
typealias MonoKProc<A> = (MonoKConnection, (Either<Throwable, A>) -> Unit) -> Unit
typealias MonoKProcF<A> = (MonoKConnection, (Either<Throwable, A>) -> Unit) -> MonoKOf<Unit>

/**
 * Connection for [MonoK].
 *
 * A connection is represented by a composite of `cancel` functions,
 * [KindConnection.cancel] is idempotent and all methods are thread-safe & atomic.
 *
 * The cancellation functions are maintained in a stack and executed in a FIFO order.
 *
 * @see MonoK.async
 */
@Suppress("UNUSED_PARAMETER", "FunctionName")
fun MonoKConnection(dummy: Unit = Unit): KindConnection<ForMonoK> = KindConnection(object : MonadDefer<ForMonoK> {
  override fun <A> defer(fa: () -> MonoKOf<A>): MonoK<A> =
    MonoK.defer(fa)

  override fun <A> raiseError(e: Throwable): MonoK<A> =
    MonoK.raiseError(e)

  override fun <A> MonoKOf<A>.handleErrorWith(f: (Throwable) -> MonoKOf<A>): MonoK<A> =
    fix().handleErrorWith(f)

  override fun <A> just(a: A): MonoK<A> =
    MonoK.just(a)

  override fun <A, B> MonoKOf<A>.flatMap(f: (A) -> MonoKOf<B>): MonoK<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: (A) -> MonoKOf<Either<A, B>>): MonoK<B> =
    MonoK.tailRecM(a, f)

  override fun <A, B> MonoKOf<A>.bracketCase(release: (A, ExitCase<Throwable>) -> MonoKOf<Unit>, use: (A) -> MonoKOf<B>): MonoK<B> =
    fix().bracketCase(release = release, use = use)
}) { it.value().subscribe({}, {}) }
