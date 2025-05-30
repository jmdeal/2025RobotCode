package frc.robot.algaepivot;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.MagnetSensorConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import edu.wpi.first.math.controller.ArmFeedforward;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import java.util.function.Supplier;

public class AlgaeSubsystem extends SubsystemBase {
    public static class Settings {
        static final int kTalonPivotID = 14;
        static final int kCANcoderPivotID = 25;

        static final double kG = 0.19; // V
        static final double kS = 0.0; // V / rad
        static final double kV = 0; // V * sec / rad
        static final double kA = 0; // V * sec^2 / rad
//        static final double kV = 1.77; // V * sec / rad
//        static final double kA = 0.01; // V * sec^2 / rad

        static final Rotation2d kMaxVelocity = Rotation2d.fromDegrees(1500);
        static final Rotation2d kMaxAcceleration = Rotation2d.fromDegrees(1500);
        static final double kP = 15.0;
        static final double kI = 0.0;
        static final double kD = 0.0;

        static final double kZeroOffset = 0.425537109375; // rotations
        // 0.129150390625
        //0.425537109375

        static final double kCurrentLimit = 40.0;

        // TODO: Enable lower min-pos to bring down CoG when elevator is up. We should be able to tuck the shooter into the elevator.
        static final Rotation2d kMinPos = Rotation2d.fromRotations(0.0439453125);
        static final Rotation2d kMaxPos = Rotation2d.fromRotations(0.369384765625);
    }

    public enum State {
        kFloorIntake(Rotation2d.fromRotations(0.05)),
        kLoliPop(Rotation2d.fromRotations(0.15)),
        kProcessor(Rotation2d.fromRotations(0.14306640625)),
        kReefIntake(Rotation2d.fromRotations(0.11938476562499999)),
        kScore(Rotation2d.fromRotations(0.27578125)),
        kStow(Rotation2d.fromRotations(0.27578125)),
        kClimb(Rotation2d.fromRotations(0.368896484375)),
        kTuck(Settings.kMaxPos);

        State(Rotation2d pos) {
            this.pos = pos;
        }
        public final Rotation2d pos;
    }

    private final TalonFX mTalonPivot;
    private final CANcoder mCANcoderPivot;
    private final ArmFeedforward mFFController;
    private final ProfiledPIDController mPPIDController;

    public static State kLastState;

    private AlgaeSubsystem() {
        mTalonPivot = new TalonFX(Settings.kTalonPivotID);
        mTalonPivot.getConfigurator().apply(new TalonFXConfiguration().withMotorOutput(new MotorOutputConfigs()
                .withInverted(InvertedValue.Clockwise_Positive)
                .withNeutralMode(NeutralModeValue.Brake)
        ));
        mTalonPivot.getConfigurator().apply(new CurrentLimitsConfigs().withSupplyCurrentLimit(Settings.kCurrentLimit));

        mCANcoderPivot = new CANcoder(Settings.kCANcoderPivotID);
        mCANcoderPivot.getConfigurator().apply(new CANcoderConfiguration().withMagnetSensor(new MagnetSensorConfigs().
                withSensorDirection(SensorDirectionValue.Clockwise_Positive).
                withMagnetOffset(Settings.kZeroOffset)
        ));

        mFFController = new ArmFeedforward(Settings.kS, Settings.kG, Settings.kV, Settings.kA);
        mPPIDController = new ProfiledPIDController(Settings.kP, Settings.kI, Settings.kD, new TrapezoidProfile.Constraints(
                Settings.kMaxVelocity.getRadians(),
                Settings.kMaxAcceleration.getRadians()
        ));
        
        if (kLastState == null) {
            kLastState = State.kStow;
        }
        mPPIDController.setGoal(kLastState.pos.getRadians());
    }


    private static AlgaeSubsystem mInstance;
    public static AlgaeSubsystem getInstance() {
        if (mInstance == null) {
            mInstance = new AlgaeSubsystem();
        }
        return mInstance;
    }

    public void setTargetState(State targetState) {
        kLastState = targetState;
        setTargetPosition(targetState.pos);
    }

    public void setTargetPosition(Rotation2d targetPosition) {
        // NOTE: Use radians for target goal to align with re:calc constant units
        mPPIDController.setGoal(targetPosition.getRadians());
    }

    public Rotation2d getWristPosition() {
        var pos = mCANcoderPivot.getAbsolutePosition().getValueAsDouble();
        return Rotation2d.fromRotations(pos);
    }

    public Rotation2d getWristVelocity() {
        var vel = mCANcoderPivot.getVelocity().getValueAsDouble();
        return Rotation2d.fromRotations(vel);
    }

    @Override
    public void periodic() {
        double voltage;
        if (kLastState != null) {
            voltage = mPPIDController.calculate(getWristPosition().getRadians());
            // voltage += mFFController.calculate(getWristPosition().getRadians(), mPPIDController.getSetpoint().velocity);
        } else {
            voltage = 0.0;
        }

        mTalonPivot.setVoltage(voltage);

        // Telemetry
        SmartDashboard.putNumber("Algae Pivot Pos (rotations)", getWristPosition().getRotations());
        SmartDashboard.putNumber("Algae Pivot Target Pos (rotations)", Rotation2d.fromRadians(mPPIDController.getSetpoint().position).getRotations());
        SmartDashboard.putNumber("Algae Pivot Vel (rotations / sec)", getWristVelocity().getRotations());
        SmartDashboard.putNumber("Algae Pivot Target Vel (rotations / sec)", Rotation2d.fromRadians(mPPIDController.getSetpoint().velocity).getRotations());
        SmartDashboard.putNumber("Algae Pivot Applied Voltage", voltage);
    }

    public static class DefaultCommand extends Command {

        public DefaultCommand() {
            addRequirements(AlgaeSubsystem.getInstance());
        }

        @Override
        public void execute() {
            if (kLastState == State.kTuck) {
                AlgaeSubsystem.getInstance().setTargetState(State.kTuck);
            } else {
                AlgaeSubsystem.getInstance().setTargetState(State.kStow);
            }
        }

    }

}
