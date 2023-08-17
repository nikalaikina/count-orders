package com.github.nikalaikina

import com.github.nikalaikina.{Interval, Order, Product}
import cats.effect.*
import cats.implicits.*
import fs2.Stream

import java.time.LocalDate.ofInstant
import java.time.ZoneOffset.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.MONTHS
import java.time.{Instant, LocalDate, ZoneOffset}
import java.util.UUID
import scala.concurrent.duration.*

object Main extends IOApp {

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      from <- IO.delay(LocalDate.parse(args(0), formatter))
      to <- IO.delay(LocalDate.parse(args(1), formatter))

      fakeRepos <- IO.delay(FakeRepos.create[IO])

      (orders, products) = fakeRepos

      reportService = OrdersReport(orders, products)

      map <- reportService.report(from, to)

      _ <- map.toList.traverse_ { (interval, k) =>
        IO.consoleForIO.println(s"${interval.show}: $k orders")
      }
    } yield ExitCode.Success
  }
}
