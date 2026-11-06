package oathdigital.gameplay.walker

import oathdigital.model._

/** The start checks procedures share. */
object ProcedureStart:
  /** Refuses a start selection handed to `name`, a procedure that makes
    * none: silently ignoring one would let a client attach a selection and
    * have the command succeed as though it had not. `name` is the subject
    * of the refusal, "the World" or "search". */
  def noSelection(name: String, args: Vector[DecisionOptionRef])
      : Either[OathViolation, Unit] = Either.cond(args.isEmpty, (),
    OathViolation.InvalidEventOrder(s"$name takes no start selection"))
