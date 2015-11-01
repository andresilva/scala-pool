package io.github.andrebeat.pool

import java.lang.ref.{ SoftReference, WeakReference }

sealed private[pool] trait Ref[A <: AnyRef] {
  def toOption(): Option[A]
}

final private[pool] class StrongRef[A <: AnyRef](val a: A) extends Ref[A] {
  def toOption() = Some(a)
}

final private[pool] class SoftRef[A <: AnyRef](val a: SoftReference[A]) extends Ref[A] {
  def toOption() = Option(a.get())
}

final private[pool] class WeakRef[A <: AnyRef](val a: WeakReference[A]) extends Ref[A] {
  def toOption() = Option(a.get())
}

private[pool] object Ref {
  def apply[A <: AnyRef](a: A, t: ReferenceType) =
    t match {
      case Strong => new StrongRef(a)
      case Soft => new SoftRef(new SoftReference(a))
      case Weak => new WeakRef(new WeakReference(a))
    }
}

/**
  * An enum-type for Java reference types.
  */
sealed trait ReferenceType
case object Strong extends ReferenceType
case object Soft extends ReferenceType
case object Weak extends ReferenceType
