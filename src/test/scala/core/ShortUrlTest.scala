package core

import org.scalameter.api._

object ShortUrlTest
  extends PerformanceTest.Quickbenchmark {
  val sizes = Gen.range("size")(300000, 1500000, 300000)

  def getId(size: Int) = scala.util.Random.nextInt(size)

  val ranges = for {
    size <- sizes
  } yield 0 until size

  def delete[A](v: Vector[A], n: Int): Vector[A] = {
    val b = v.companion.newBuilder[A]
    var i = 0
    v.foreach {
      x =>
        if (i != n) b += x
        i += 1
    }
    b.result()
  }

  performance of "Range" in {

    measure method "delete" in {
      using(ranges) in {
        r =>
          val s = r.toVector
          val id = getId(s.size)
          val sNew = delete(s, id)
      }
    }
    measure method "vector" in {
      using(ranges) in {
        r =>
          val s = r.toVector
          val id = getId(s.size)
          val sNew = (s take id) ++ (s drop (id + 1))
      }
    }
  }
}