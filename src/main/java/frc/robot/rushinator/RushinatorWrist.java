package frc.robot.rushinator;

import static edu.wpi.first.units.Units.Rotation;

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
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.rushinator.commands.SetWristState;

public class RushinatorWrist extends SubsystemBase {
    public static class Settings {
        static final int kTalonWristID = 12; 
        static final int kCancoderWristID = 24; 

        public static final double kG = 0.01; // V
        public static final double kS = 0.0; // V / rad
        public static final double kV = 0.5; // V * sec / rad
        public static final double kA = 0.01; // V * sec^2 / rad

        public static final Rotation2d kMaxVelocity = Rotation2d.fromDegrees(300000);
        public static final Rotation2d kMaxAcceleration = Rotation2d.fromDegrees(300000);
        public static final double kP = 50.0;
        public static final double kI = 0.0;
        public static final double kD = 0.0;

        static final double kCurrentLimit = 40.0;

        public static final double kZeroOffset = 0.505126953125; // rotations

    }
// 12.3720703125 Score MId
/*
 * kScoreLeftWrist(Rotation2d.fromRotations(15.62573242 + 23)),
        kScoreRightWrist(Rotation2d.fromRotations(15.62573242 - 23)),
        kScoreMid(Rotation2d.fromRotations(15.62573242)),
        kScoreL1LeftWrist(Rotation2d.fromRotations(10.52197265625 + 23)),
        kScoreL1RightWrist(Rotation2d.fromRotations(10.52197265625 - 23)),
        kScoreL1Mid(Rotation2d.fromRotations(10.52197265625)),
        kHPLeft(Rotation2d.fromRotations(25.22802734375 + 23)),
        kHPRight(Rotation2d.fromRotations(25.22802734375 - 23)),
        kHPMid(Rotation2d.fromRotations(25.22802734375)),
        kGroundLeft(Rotation2d.fromRotations(-6.48388671875 + 23)),
        kGroundRight(Rotation2d.fromRotations(-6.48388671875 - 23)),
        kGroundMid(Rotation2d.fromRotations(-6.48388671875)),
        kTravelLeft(Rotation2d.fromRotations(2.5478515625 + 23)),
        kTravelRight(Rotation2d.fromRotations(2.5478515625 - 23)),
        kTravelMid(Rotation2d.fromRotations(2.5478515625));
 */
    public enum State {
        kScoreLeftWrist(Rotation2d.fromRotations(-0.13427734375 - 0.25)),
        kScoreRightWrist(Rotation2d.fromRotations(-0.13427734375 + 0.25)),
        kScoreMid(Rotation2d.fromRotations(-0.13427734375)),
        kScoreL4LeftWrist(Rotation2d.fromRotations(0.09375 - 0.50)),
        kScoreL4RightWrist(Rotation2d.fromRotations(0.09375)),
        kScoreL4Mid(Rotation2d.fromRotations(0.09375 - 0.25)),
        kScoreL3LeftWrist(Rotation2d.fromRotations(-0.102783203125 - 0.25)),
        kScoreL3RightWrist(Rotation2d.fromRotations(-0.102783203125 + 0.25)),
        kScoreL3Mid(Rotation2d.fromRotations(-0.102783203125)),
        kScoreL2LeftWrist(Rotation2d.fromRotations(-0.056396484375 - 0.25)),
        kScoreL2RightWrist(Rotation2d.fromRotations(-0.056396484375 + 0.25)),
        kScoreL2Mid(Rotation2d.fromRotations(-0.056396484375)),
        kScoreL1LeftWrist(Rotation2d.fromRotations(-0.12109375 - 0.25)),
        kScoreL1RightWrist(Rotation2d.fromRotations(-0.12109375 + 0.25)),
        kScoreL1Mid(Rotation2d.fromRotations(-0.12109375)),
        kHPLeft(Rotation2d.fromRotations(-0.24755859375 - 0.25)),
        kHPRight(Rotation2d.fromRotations(-0.24755859375 + 0.25)),
        kHPMid(Rotation2d.fromRotations(-0.24755859375)),
        kGroundLeft(Rotation2d.fromRotations(0.07275390625 - 0.25)),
        kGroundRight(Rotation2d.fromRotations(0.07275390625+ 0.25)),
        kGroundMid(Rotation2d.fromRotations(0.07275390625)),
        kLoliLeft(Rotation2d.fromRotations(0.027587890625 - 0.25)),
        kLoliRight(Rotation2d.fromRotations(0.027587890625+ 0.25)),
        kLoliMid(Rotation2d.fromRotations(0.027587890625)),
        kTravelLeft(Rotation2d.fromRotations(-0.25 - 0.25)),
        kTravelRight(Rotation2d.fromRotations(-0.25 + 0.25)),
        kTravelMid(Rotation2d.fromRotations(-0.25)),
        kTravelL4Left(Rotation2d.fromRotations(-0.191650390625 - 0.25)),
        kTravelL4Right(Rotation2d.fromRotations(-0.191650390625 + 0.25)),
        kTravelL4Mid(Rotation2d.fromRotations(-0.191650390625)),
        kTravelAutoAlignL4Left(Rotation2d.fromRotations(-0.191650390625 - 0.25)),
        kTravelAutonAlignL4Right(Rotation2d.fromRotations(-0.191650390625 + 0.25)),
        kTravelAutonAlignL4Mid(Rotation2d.fromRotations(-0.191650390625)),
        kClimbLeft(Rotation2d.fromRotations(-0.28759765625 - 0.25)),
        kClimblRight(Rotation2d.fromRotations(-0.28759765625 + 0.25)),
        kClimblMid(Rotation2d.fromRotations(-0.28759765625));

        State(Rotation2d pos) {
            this.pos = pos;
        }
        public final Rotation2d pos;
    }


    private final CANcoder mWristCancoder;
    public TalonFX mWristTalon;
    private final ProfiledPIDController mPPIDController;
    private final SimpleMotorFeedforward mFFController;
    private final PIDController mPIDController;
    
    // private final ArmFeedforward mFFController;
    
    public static State kLastState;
    
    public RushinatorWrist() {  
        mWristTalon = new TalonFX(Settings.kTalonWristID);
        mWristTalon.getConfigurator().apply(new TalonFXConfiguration().withMotorOutput(new MotorOutputConfigs()
                .withInverted(InvertedValue.CounterClockwise_Positive)
                .withNeutralMode(NeutralModeValue.Brake)
        ));
        mWristTalon.getConfigurator().apply(new CurrentLimitsConfigs().withSupplyCurrentLimit(Settings.kCurrentLimit));

        mWristCancoder =  new CANcoder(Settings.kCancoderWristID);
        mWristCancoder.getConfigurator().apply(new CANcoderConfiguration().withMagnetSensor(new MagnetSensorConfigs().
                withSensorDirection(SensorDirectionValue.Clockwise_Positive).
                withMagnetOffset(Settings.kZeroOffset)
        ));

        mPPIDController = new ProfiledPIDController(Settings.kP, Settings.kI, Settings.kD, new TrapezoidProfile.Constraints(
                Settings.kMaxVelocity.getRadians(),
                Settings.kMaxAcceleration.getRadians()
        ));
        mPPIDController.setTolerance(0.01); //degrees of tolerance

        mPIDController = new PIDController(Settings.kP, Settings.kI, Settings.kD);
        // mFFController = new ArmFeedforward(Settings.kS, Settings.kG, Settings.kV, Settings.kA);

        mFFController = new SimpleMotorFeedforward(Settings.kS, Settings.kV, Settings.kA);

        if (kLastState == null) {
            kLastState = State.kTravelRight;
        }
        mPPIDController.setGoal(kLastState.pos.getRotations());
    }

    private static RushinatorWrist mInstance;
    public static RushinatorWrist getInstance() {
        if (mInstance == null) {
            mInstance = new RushinatorWrist();
        }
        return mInstance;
    }

    public Rotation2d getWristRelativePos() {
        return Rotation2d.fromRotations(mWristTalon.getPosition().getValueAsDouble());
    }

    public void setTargetState(State targetState) {
        kLastState = targetState;
        setTargetPosition(targetState.pos);
    }

    public void setTargetPosition(Rotation2d targetPosition) {
        mPPIDController.setGoal(targetPosition.getRotations());
        mPIDController.setSetpoint(targetPosition.getRotations());
    }

    public void setVoltage(double voltage) {
        mWristTalon.setVoltage(voltage);
    }

    public double getMotorOutputVoltage() {
        return mWristTalon.getMotorVoltage().getValueAsDouble();
    }

    public boolean atSetpoint() {
        return mPPIDController.atGoal();
    }

    public State getCurrentWristState() {
        return kLastState;
    }

    public Rotation2d getCurrentPos() {
        return Rotation2d.fromRotations(mWristCancoder.getPosition().getValueAsDouble());
    }

    public Rotation2d getCurrentRelativePos() {
        return Rotation2d.fromRotations(mWristTalon.getPosition().getValueAsDouble());
    }

    @Override
    public void periodic() {
        double pidOutput = mPPIDController.calculate(getCurrentPos().getRotations());
        // double pidOutput = mPIDController.calculate(getCurrentPos().getRotations());
        // double ffOutput = mFFController.calculate(getWristRelativePos().getRotations(), mPPIDController.getSetpoint().velocity);
        double ffOutput = mFFController.calculate(mPPIDController.getSetpoint().velocity);
        double totalOutputVoltage = pidOutput + ffOutput;
        mWristTalon.setVoltage(-totalOutputVoltage);
        

        SmartDashboard.putNumber("PID Output", pidOutput);
        SmartDashboard.putNumber("FF Output", ffOutput);
        SmartDashboard.putNumber("Output Voltage", totalOutputVoltage);
        SmartDashboard.putString("KLastState Wrist Pivot", kLastState.name());
        SmartDashboard.putNumber("Coral Wrist Current Angle (Rotations)", getCurrentPos().getRotations());
        SmartDashboard.putNumber("Coral Wrist Pivot (Rotations Relavtive)", mWristTalon.getPosition().getValueAsDouble());
        SmartDashboard.putNumber("Coral WRist Current Vel", mWristTalon.getVelocity().getValueAsDouble());

        SmartDashboard.putNumber("Coral Wrist Target Pos", mPPIDController.getSetpoint().position);
        SmartDashboard.putNumber("Coral Wrist Target Vel", mPPIDController.getSetpoint().velocity);
    }

    public static class DefaultCommand extends Command {
        RushinatorWrist subsystem;
        public DefaultCommand() {
            this.subsystem = RushinatorWrist.getInstance();
            addRequirements(subsystem);
        }

        @Override
        public void execute() {
            // if (kLastState == RushinatorWrist.State.kScoreRightWrist) {
            //     new SetWristState(RushinatorWrist.State.kTravelRight);
            // } else if (kLastState == RushinatorWrist.State.kScoreLeftWrist){
            //     new SetWristState(RushinatorWrist.State.kTravelLeft);
            // } else if (kLastState == RushinatorWrist.State.kTravelLeft){
            //     new SetWristState(RushinatorWrist.State.kTravelLeft);
            // } else if (kLastState == RushinatorWrist.State.kTravelRight) {
            //     new SetWristState(RushinatorWrist.State.kTravelRight);
            // } else {
            //     new SetWristState(RushinatorWrist.State.kTravelRight);
            // }
            RushinatorWrist.getInstance().mPPIDController.setGoal(RushinatorWrist.State.kTravelRight.pos.getRotations());
        }
    }
}
