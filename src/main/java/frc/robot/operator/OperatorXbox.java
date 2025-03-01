package frc.robot.operator;

import edu.wpi.first.math.estimator.PoseEstimator;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.ConditionalCommand;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import frc.crevolib.util.ExpCurve;
import frc.crevolib.util.Gamepad;
import frc.crevolib.util.XboxGamepad;
import frc.robot.Robot;
import frc.robot.algaeflywheel.AlgaeRoller;
import frc.robot.algaepivot.AlgaeSubsystem;
import frc.robot.algaepivot.commands.AlgaePivotCommands;
import frc.robot.algaepivot.commands.SetAngleAlgaePivot;
import frc.robot.indexer.commands.IndexerCommands;
import frc.robot.subsystems.CoralRollerSubsystem;
import frc.robot.subsystems.CoralSubsystem;

public class OperatorXbox extends XboxGamepad {
    private static class Settings {
        static final int port = 1;
        static final String name = "operator";

        static final double kDeadzone = 0.1;
    }

    ExpCurve stickCurve;
    ExpCurve shooterPivotManualCurve;
    ExpCurve intakePivotManualCurve;
    ExpCurve positionTestCurve;
    ExpCurve elevatorCurve;
    private static OperatorXbox mInstance;

    ExpCurve shooterManualCurve;

    private OperatorXbox() {
        super(Settings.name, Settings.port);

        stickCurve = new ExpCurve(1, 0, 1, Settings.kDeadzone);
    }

    public static OperatorXbox getInstance() {
        if (mInstance == null) {
            mInstance = new OperatorXbox();
        }
        return mInstance;
    }

    @Override
    public void setupTeleopButtons() {
//        controller.leftBumper().whileTrue(IndexerCommands.setOutput(() -> -1.0));
//
//        controller.leftTrigger().whileTrue(RobotCommands.primeShoot());
//        controller.leftTrigger().whileTrue(IndexerCommands.setOutput(() -> 1.0));
        controller.leftTrigger().whileTrue(new SetAngleAlgaePivot(AlgaeSubsystem.State.kFloorIntake));
        controller.leftTrigger().whileTrue(new AlgaeRoller.PrimeCommand());
        controller.leftBumper().whileTrue(new AlgaeRoller.ShootCommand());
//        controller.leftBumper().whileTrue(new AlgaeRoller.ShootCommand());
//
        controller.rightTrigger().whileTrue(new CoralSubsystem.SetStateCommand(CoralSubsystem.State.kScoreV2));
        controller.rightBumper().whileTrue(new CoralRollerSubsystem.SetVoltageCommand(-12));
    }

    @Override
    public void setupDisabledButtons() {

    }

    @Override
    public void setupTestButtons() {
    }



    public Translation2d getDriveTranslation() {
        final var xComponent = stickCurve.calculate(controller.getLeftX());
        final var yComponent = stickCurve.calculate(controller.getLeftY());
        // Components are reversed because field coordinates are opposite of joystick coordinates
        return new Translation2d(yComponent, xComponent);
    }

    public double getElevatorOutput() {
        return stickCurve.calculate(-controller.getRightY());
    }

    public double getDriveRotation() {
        return -stickCurve.calculate(-controller.getRightX());
    }

}
