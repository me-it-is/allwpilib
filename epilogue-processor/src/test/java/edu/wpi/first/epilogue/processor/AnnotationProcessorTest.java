// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package edu.wpi.first.epilogue.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static edu.wpi.first.epilogue.processor.CompileTestOptions.kJavaVersionOptions;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:LineLength") // Source code templates exceed the line length limit
class AnnotationProcessorTest {
  @Test
  void simple() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        double x;
      }
    """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", double.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((double) $x.get(object)));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void optInFields() {
    String source =
        """
      package edu.wpi.first.epilogue;

      class Example {
        @Logged double x;
        @Logged int y;
      }
    """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;
        private static final VarHandle $y;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", double.class);
            $y = lookup.findVarHandle(Example.class, "y", int.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((double) $x.get(object)));
            backend.log("y", ((int) $y.get(object)));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void optInMethods() {
    String source =
        """
      package edu.wpi.first.epilogue;

      class Example {
        @Logged public double getValue() { return 2.0; }
        @Logged public String getName() { return "Example"; }
      }
    """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("getValue", object.getValue());
            backend.log("getName", object.getName());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void shouldNotLog() {
    String source =
        """
      class Example {
        public double getValue() { return 2.0; }
        public String getName() { return "Example"; }
      }
    """;

    Compilation compilation =
        javac()
            .withOptions(kJavaVersionOptions)
            .withProcessors(new AnnotationProcessor())
            .compile(JavaFileObjects.forSourceString("edu.wpi.first.epilogue.Example", source));

    assertThat(compilation).succeeded();
    // nothing is annotated with @Logged; so, no logger file should be generated
    assertTrue(
        compilation.generatedSourceFiles().stream()
            .noneMatch(jfo -> jfo.getName().contains("Example")));
  }

  @Test
  void multiple() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        double x;
        double y;
      }
    """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;
        private static final VarHandle $y;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", double.class);
            $y = lookup.findVarHandle(Example.class, "y", double.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((double) $x.get(object)));
            backend.log("y", ((double) $y.get(object)));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void privateFields() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        private double x;
      }
    """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", double.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((double) $x.get(object)));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void privateSuppliers() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import java.util.function.DoubleSupplier;

      @Logged
      class Example {
        private DoubleSupplier x;
      }
    """;

    String expectedGeneratedSource =
        """
    package edu.wpi.first.epilogue;

    import edu.wpi.first.epilogue.Logged;
    import edu.wpi.first.epilogue.Epilogue;
    import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
    import edu.wpi.first.epilogue.logging.EpilogueBackend;
    import java.lang.invoke.MethodHandles;
    import java.lang.invoke.VarHandle;

    public class ExampleLogger extends ClassSpecificLogger<Example> {
      private static final VarHandle $x;

      static {
        try {
          var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
          $x = lookup.findVarHandle(Example.class, "x", java.util.function.DoubleSupplier.class);
        } catch (ReflectiveOperationException e) {
          throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
        }
      }

      public ExampleLogger() {
        super(Example.class);
      }

      @Override
      public void update(EpilogueBackend backend, Example object) {
        if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
          backend.log("x", ((java.util.function.DoubleSupplier) $x.get(object)).getAsDouble());
        }
      }
    }
    """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void privateWithGenerics() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        private edu.wpi.first.wpilibj.smartdashboard.SendableChooser<String> chooser;
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $chooser;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $chooser = lookup.findVarHandle(Example.class, "chooser", edu.wpi.first.wpilibj.smartdashboard.SendableChooser.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            logSendable(backend.getNested("chooser"), ((edu.wpi.first.wpilibj.smartdashboard.SendableChooser<java.lang.String>) $chooser.get(object)));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void importanceLevels() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged(importance = Logged.Importance.INFO)
      class Example {
        @Logged(importance = Logged.Importance.DEBUG)    double low;
        @Logged(importance = Logged.Importance.INFO)     int    medium;
        @Logged(importance = Logged.Importance.CRITICAL) long   high;
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $low;
        private static final VarHandle $medium;
        private static final VarHandle $high;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $low = lookup.findVarHandle(Example.class, "low", double.class);
            $medium = lookup.findVarHandle(Example.class, "medium", int.class);
            $high = lookup.findVarHandle(Example.class, "high", long.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("low", ((double) $low.get(object)));
          }
          if (Epilogue.shouldLog(Logged.Importance.INFO)) {
            backend.log("medium", ((int) $medium.get(object)));
          }
          if (Epilogue.shouldLog(Logged.Importance.CRITICAL)) {
            backend.log("high", ((long) $high.get(object)));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void logEnum() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        enum E {
          a, b, c;
        }
        E enumValue;   // Should be logged
        E[] enumArray; // Should not be logged
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $enumValue;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $enumValue = lookup.findVarHandle(Example.class, "enumValue", edu.wpi.first.epilogue.Example.E.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("enumValue", ((edu.wpi.first.epilogue.Example.E) $enumValue.get(object)));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void superclassStillOptIn() {
    String source =
        """
      package edu.wpi.first.epilogue;

      // nothing should be logged from BaseExample
      class BaseExample {
        public double x;
        public double getValue() { return 2.0; }
      }

      @Logged
      class Example extends BaseExample {
        double y;
      }
    """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $y;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $y = lookup.findVarHandle(Example.class, "y", double.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("y", ((double) $y.get(object)));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void superclass() {
    String source =
        """
      package edu.wpi.first.epilogue;

      class Grandparent {
        @Logged
        public double a;
        @Logged public double getB() { return 0; }
        public double getC() { return 1; }             // not annotated, not logged
      }

      @Logged
      class BaseExample extends Grandparent {
        protected double d;
        public double e;
        private double f;
        double g;
        public double getValue() { return 2.0; }
        private double getOtherValue() { return 3.0; }  // private, not logged
      }

      @Logged
      class Example extends BaseExample {
        double h;
      }
    """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $h;
        private static final VarHandle $d;
        private static final VarHandle $f;
        private static final VarHandle $g;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $h = lookup.findVarHandle(Example.class, "h", double.class);
            $d = lookup.findVarHandle(Example.class, "d", double.class);
            $f = lookup.findVarHandle(Example.class, "f", double.class);
            $g = lookup.findVarHandle(Example.class, "g", double.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("h", ((double) $h.get(object)));
            backend.log("d", ((double) $d.get(object)));
            backend.log("e", object.e);
            backend.log("f", ((double) $f.get(object)));
            backend.log("g", ((double) $g.get(object)));
            backend.log("a", object.a);
            backend.log("getValue", object.getValue());
            backend.log("getB", object.getB());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void bytes() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        byte x;        // Should be logged
        byte[] arr1;   // Should be logged
        byte[][] arr2; // Should not be logged

        public byte getX() { return 0; }
        public byte[] getArr1() { return null; }
        public byte[][] getArr2() { return null; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;
        private static final VarHandle $arr1;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", byte.class);
            $arr1 = lookup.findVarHandle(Example.class, "arr1", byte[].class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((byte) $x.get(object)));
            backend.log("arr1", ((byte[]) $arr1.get(object)));
            backend.log("getX", object.getX());
            backend.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void chars() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        char x;        // Should be logged
        char[] arr1;   // Should not be logged
        char[][] arr2; // Should not be logged

        public char getX() { return 'x'; }
        public char[] getArr1() { return null; }
        public char[][] getArr2() { return null; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", char.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((char) $x.get(object)));
            backend.log("getX", object.getX());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void shorts() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        short x;        // Should be logged
        short[] arr1;   // Should not be logged
        short[][] arr2; // Should not be logged

        public short getX() { return 0; }
        public short[] getArr1() { return null; }
        public short[][] getArr2() { return null; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", short.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((short) $x.get(object)));
            backend.log("getX", object.getX());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void ints() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        int x;           // Should be logged
        int[] arr1;   // Should be logged
        int[][] arr2; // Should not be logged

        public int getX() { return 0; }
        public int[] getArr1() { return null; }
        public int[][] getArr2() { return null; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;
        private static final VarHandle $arr1;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", int.class);
            $arr1 = lookup.findVarHandle(Example.class, "arr1", int[].class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((int) $x.get(object)));
            backend.log("arr1", ((int[]) $arr1.get(object)));
            backend.log("getX", object.getX());
            backend.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void longs() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        long x;        // Should be logged
        long[] arr1;   // Should be logged
        long[][] arr2; // Should not be logged

        public long getX() { return 0; }
        public long[] getArr1() { return null; }
        public long[][] getArr2() { return null; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;
        private static final VarHandle $arr1;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", long.class);
            $arr1 = lookup.findVarHandle(Example.class, "arr1", long[].class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((long) $x.get(object)));
            backend.log("arr1", ((long[]) $arr1.get(object)));
            backend.log("getX", object.getX());
            backend.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void floats() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example {
        float x;        // Should be logged
        float[] arr1;   // Should be logged
        float[][] arr2; // Should not be logged

        public float getX() { return 0; }
        public float[] getArr1() { return null; }
        public float[][] getArr2() { return null; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;
        private static final VarHandle $arr1;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", float.class);
            $arr1 = lookup.findVarHandle(Example.class, "arr1", float[].class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((float) $x.get(object)));
            backend.log("arr1", ((float[]) $arr1.get(object)));
            backend.log("getX", object.getX());
            backend.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void doubles() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import java.util.List;

      @Logged
      class Example {
        double x;        // Should be logged
        double[] arr1;   // Should be logged
        double[][] arr2; // Should not be logged
        List<Double> list; // Should not be logged

        public double getX() { return 0; }
        public double[] getArr1() { return null; }
        public double[][] getArr2() { return null; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;
        private static final VarHandle $arr1;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", double.class);
            $arr1 = lookup.findVarHandle(Example.class, "arr1", double[].class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((double) $x.get(object)));
            backend.log("arr1", ((double[]) $arr1.get(object)));
            backend.log("getX", object.getX());
            backend.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void booleans() {
    String source =
        """
      package edu.wpi.first.epilogue;
      import java.util.List;

      @Logged
      class Example {
        boolean x;        // Should be logged
        boolean[] arr1;   // Should be logged
        boolean[][] arr2; // Should not be logged
        List<Boolean> list; // Should not be logged

        public boolean getX() { return false; }
        public boolean[] getArr1() { return null; }
        public boolean[][] getArr2() { return null; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;
        private static final VarHandle $arr1;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", boolean.class);
            $arr1 = lookup.findVarHandle(Example.class, "arr1", boolean[].class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((boolean) $x.get(object)));
            backend.log("arr1", ((boolean[]) $arr1.get(object)));
            backend.log("getX", object.getX());
            backend.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void strings() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import java.util.List;

      @Logged
      class Example {
        String x;         // Should be logged
        String[] arr1;   // Should be logged
        String[][] arr2; // Should not be logged
        List<String> list;  // Should be logged

        public String getX() { return null; }
        public String[] getArr1() { return null; }
        public String[][] getArr2() { return null; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;
        private static final VarHandle $arr1;
        private static final VarHandle $list;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", java.lang.String.class);
            $arr1 = lookup.findVarHandle(Example.class, "arr1", java.lang.String[].class);
            $list = lookup.findVarHandle(Example.class, "list", java.util.List.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((java.lang.String) $x.get(object)));
            backend.log("arr1", ((java.lang.String[]) $arr1.get(object)));
            backend.log("list", ((java.util.List<java.lang.String>) $list.get(object)));
            backend.log("getX", object.getX());
            backend.log("getArr1", object.getArr1());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void structs() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.List;

      @Logged
      class Example {
        static class Structable implements StructSerializable {
          int x, y;

          public static final Struct<Structable> struct = null; // value doesn't matter
        }

        Structable x;        // Should be logged
        Structable[] arr1;   // Should be logged
        Structable[][] arr2; // Should not be logged
        List<Structable> list; // Should be logged

        public Structable getX() { return null; }
        public Structable[] getArr1() { return null; }
        public Structable[][] getArr2() { return null; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $x;
        private static final VarHandle $arr1;
        private static final VarHandle $list;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $x = lookup.findVarHandle(Example.class, "x", edu.wpi.first.epilogue.Example.Structable.class);
            $arr1 = lookup.findVarHandle(Example.class, "arr1", edu.wpi.first.epilogue.Example.Structable[].class);
            $list = lookup.findVarHandle(Example.class, "list", java.util.List.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("x", ((edu.wpi.first.epilogue.Example.Structable) $x.get(object)), edu.wpi.first.epilogue.Example.Structable.struct);
            backend.log("arr1", ((edu.wpi.first.epilogue.Example.Structable[]) $arr1.get(object)), edu.wpi.first.epilogue.Example.Structable.struct);
            backend.log("list", ((java.util.List<edu.wpi.first.epilogue.Example.Structable>) $list.get(object)), edu.wpi.first.epilogue.Example.Structable.struct);
            backend.log("getX", object.getX(), edu.wpi.first.epilogue.Example.Structable.struct);
            backend.log("getArr1", object.getArr1(), edu.wpi.first.epilogue.Example.Structable.struct);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void lists() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.*;

      @Logged
      class Example {
        /* Logged */     List<String> list;
        /* Not Logged */ List<List<String>> nestedList;
        /* Not logged */ List rawList;
        /* Logged */     Set<String> set;
        /* Logged */     Queue<String> queue;
        /* Logged */     Stack<String> stack;
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $list;
        private static final VarHandle $set;
        private static final VarHandle $queue;
        private static final VarHandle $stack;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $list = lookup.findVarHandle(Example.class, "list", java.util.List.class);
            $set = lookup.findVarHandle(Example.class, "set", java.util.Set.class);
            $queue = lookup.findVarHandle(Example.class, "queue", java.util.Queue.class);
            $stack = lookup.findVarHandle(Example.class, "stack", java.util.Stack.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("list", ((java.util.List<java.lang.String>) $list.get(object)));
            backend.log("set", ((java.util.Set<java.lang.String>) $set.get(object)));
            backend.log("queue", ((java.util.Queue<java.lang.String>) $queue.get(object)));
            backend.log("stack", ((java.util.Stack<java.lang.String>) $stack.get(object)));
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void boxedPrimitiveLists() {
    // Boxed primitives are not directly supported, nor are arrays of boxed primitives
    // int[] is fine, but Integer[] is not

    String source =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.List;

      @Logged
      class Example {
        /* Not logged */ List<Integer> ints;
        /* Not logged */ List<Double> doubles;
        /* Not logged */ List<Long> longs;
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void badLogSetup() {
    String source =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.util.struct.Struct;
      import edu.wpi.first.util.struct.StructSerializable;
      import java.util.*;

      @Logged
      class Example {
        @Logged Map<String, String> notLoggableType;
        @Logged List rawType;
        @NotLogged List skippedUnloggable;

        @Logged
        private String privateMethod() { return ""; }

        @Logged
        String packagePrivateMethod() { return ""; }

        @Logged
        protected String protectedMethod() { return ""; }

        @Logged
        public static String publicStaticMethod() { return ""; }

        @Logged
        private static String privateStaticMethod() { return ""; }

        @Logged
        public void publicVoidMethod() {}

        @Logged
        public Map<String, String> publicNonLoggableMethod() { return notLoggableType; }
      }
      """;

    Compilation compilation =
        javac()
            .withOptions(kJavaVersionOptions)
            .withProcessors(new AnnotationProcessor())
            .compile(JavaFileObjects.forSourceString("edu.wpi.first.epilogue.Example", source));

    assertThat(compilation).failed();
    assertThat(compilation).hadErrorCount(10);

    List<Diagnostic<? extends JavaFileObject>> errors = compilation.errors();
    assertAll(
        () ->
            assertCompilationError(
                "[EPILOGUE] You have opted in to logging on this field, but it is not a loggable data type!",
                9,
                31,
                errors.get(0)),
        () ->
            assertCompilationError(
                "[EPILOGUE] You have opted in to logging on this field, but it is not a loggable data type!",
                10,
                16,
                errors.get(1)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods must be public", 14, 18, errors.get(2)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods must be public", 17, 10, errors.get(3)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods must be public", 20, 20, errors.get(4)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods cannot be static", 23, 24, errors.get(5)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods must be public", 26, 25, errors.get(6)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Logged methods cannot be static", 26, 25, errors.get(7)),
        () ->
            assertCompilationError(
                "[EPILOGUE] You have opted in to logging on this method, but it does not return a loggable data type!",
                29,
                15,
                errors.get(8)),
        () ->
            assertCompilationError(
                "[EPILOGUE] You have opted in to logging on this method, but it does not return a loggable data type!",
                32,
                30,
                errors.get(9)));
  }

  @Test
  void onGenericType() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Example<T extends String> {
        T value;

        public <S extends T> S upcast() { return (S) value; }
      }
      """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $value;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $value = lookup.findVarHandle(Example.class, "value", java.lang.String.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            backend.log("value", ((java.lang.String) $value.get(object)));
            backend.log("upcast", object.upcast());
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void annotationInheritance() {
    String source =
        """
      package edu.wpi.first.epilogue;

      @Logged
      class Child {}

      class GoldenChild extends Child {} // inherits the @Logged annotation from the parent

      @Logged
      interface IO {}

      class IOImpl implements IO {}

      @Logged
      public class Example {
        /* Logged */     Child child;
        /* Not Logged */ GoldenChild goldenChild;
        /* Logged */     IO io;
        /* Not logged */ IOImpl ioImpl;
      }
      """;

    String expectedRootLogger =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $child;
        private static final VarHandle $io;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $child = lookup.findVarHandle(Example.class, "child", edu.wpi.first.epilogue.Child.class);
            $io = lookup.findVarHandle(Example.class, "io", edu.wpi.first.epilogue.IO.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            Epilogue.childLogger.tryUpdate(backend.getNested("child"), ((edu.wpi.first.epilogue.Child) $child.get(object)), Epilogue.getConfig().errorHandler);
            Epilogue.ioLogger.tryUpdate(backend.getNested("io"), ((edu.wpi.first.epilogue.IO) $io.get(object)), Epilogue.getConfig().errorHandler);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void inheritanceOfLoggedTypes() {
    String source =
        """
        package edu.wpi.first.epilogue;

        @Logged
        interface IFace {}

        @Logged
        class Impl1 implements IFace {}

        @Logged
        class Impl2 implements IFace {}

        @Logged
        interface I {
          int a();
        }

        @Logged
        interface I2 extends I {
          int x();
        }

        @Logged
        interface I3 extends I {
          int y();
        }

        @Logged
        interface I4 extends I2, I3 {
          int z();
        }

        @Logged
        class ConcreteLogged implements I4 {
          public int a() { return 0; }
          public int x() { return 0; }
          public int y() { return 0; }
          public int z() { return 0; }
        }

        class ConcreteNotLogged implements I4 {
          public int a() { return 0; }
          public int x() { return 0; }
          public int y() { return 0; }
          public int z() { return 0; }
        }

        @Logged
        public class Example {
          IFace asInterface;
          Impl1 firstImpl;
          Impl2 secondImpl;

          I complex;
        }
        """;

    String expectedRootLogger =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;
        import java.lang.invoke.MethodHandles;
        import java.lang.invoke.VarHandle;

        public class ExampleLogger extends ClassSpecificLogger<Example> {
          private static final VarHandle $asInterface;
          private static final VarHandle $firstImpl;
          private static final VarHandle $secondImpl;
          private static final VarHandle $complex;

          static {
            try {
              var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
              $asInterface = lookup.findVarHandle(Example.class, "asInterface", edu.wpi.first.epilogue.IFace.class);
              $firstImpl = lookup.findVarHandle(Example.class, "firstImpl", edu.wpi.first.epilogue.Impl1.class);
              $secondImpl = lookup.findVarHandle(Example.class, "secondImpl", edu.wpi.first.epilogue.Impl2.class);
              $complex = lookup.findVarHandle(Example.class, "complex", edu.wpi.first.epilogue.I.class);
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
            }
          }

          public ExampleLogger() {
            super(Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              var $$asInterface = ((edu.wpi.first.epilogue.IFace) $asInterface.get(object));
              if ($$asInterface instanceof edu.wpi.first.epilogue.Impl1 edu_wpi_first_epilogue_Impl1) {
                Epilogue.impl1Logger.tryUpdate(backend.getNested("asInterface"), edu_wpi_first_epilogue_Impl1, Epilogue.getConfig().errorHandler);
              } else if ($$asInterface instanceof edu.wpi.first.epilogue.Impl2 edu_wpi_first_epilogue_Impl2) {
                Epilogue.impl2Logger.tryUpdate(backend.getNested("asInterface"), edu_wpi_first_epilogue_Impl2, Epilogue.getConfig().errorHandler);
              } else {
                // Base type edu.wpi.first.epilogue.IFace
                Epilogue.iFaceLogger.tryUpdate(backend.getNested("asInterface"), $$asInterface, Epilogue.getConfig().errorHandler);
              };
              Epilogue.impl1Logger.tryUpdate(backend.getNested("firstImpl"), ((edu.wpi.first.epilogue.Impl1) $firstImpl.get(object)), Epilogue.getConfig().errorHandler);
              Epilogue.impl2Logger.tryUpdate(backend.getNested("secondImpl"), ((edu.wpi.first.epilogue.Impl2) $secondImpl.get(object)), Epilogue.getConfig().errorHandler);
              var $$complex = ((edu.wpi.first.epilogue.I) $complex.get(object));
              if ($$complex instanceof edu.wpi.first.epilogue.ConcreteLogged edu_wpi_first_epilogue_ConcreteLogged) {
                Epilogue.concreteLoggedLogger.tryUpdate(backend.getNested("complex"), edu_wpi_first_epilogue_ConcreteLogged, Epilogue.getConfig().errorHandler);
              } else if ($$complex instanceof edu.wpi.first.epilogue.I4 edu_wpi_first_epilogue_I4) {
                Epilogue.i4Logger.tryUpdate(backend.getNested("complex"), edu_wpi_first_epilogue_I4, Epilogue.getConfig().errorHandler);
              } else if ($$complex instanceof edu.wpi.first.epilogue.I2 edu_wpi_first_epilogue_I2) {
                Epilogue.i2Logger.tryUpdate(backend.getNested("complex"), edu_wpi_first_epilogue_I2, Epilogue.getConfig().errorHandler);
              } else if ($$complex instanceof edu.wpi.first.epilogue.I3 edu_wpi_first_epilogue_I3) {
                Epilogue.i3Logger.tryUpdate(backend.getNested("complex"), edu_wpi_first_epilogue_I3, Epilogue.getConfig().errorHandler);
              } else {
                // Base type edu.wpi.first.epilogue.I
                Epilogue.iLogger.tryUpdate(backend.getNested("complex"), $$complex, Epilogue.getConfig().errorHandler);
              };
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void innerClasses() {
    String source =
        """
        package edu.wpi.first.epilogue;

        class Outer {
          @Logged
          class Example { // Deliberately nonstatic
            double x;
          }
        }
        """;

    String expectedRootLogger =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;
        import java.lang.invoke.MethodHandles;
        import java.lang.invoke.VarHandle;

        public class Outer$ExampleLogger extends ClassSpecificLogger<Outer.Example> {
          private static final VarHandle $x;

          static {
            try {
              var lookup = MethodHandles.privateLookupIn(Outer.Example.class, MethodHandles.lookup());
              $x = lookup.findVarHandle(Outer.Example.class, "x", double.class);
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
            }
          }

          public Outer$ExampleLogger() {
            super(Outer.Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, Outer.Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              backend.log("x", ((double) $x.get(object)));
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void highlyNestedInnerClasses() {
    String source =
        """
        package edu.wpi.first.epilogue;

        class A {
          class B {
            class C {
              class D {
                @Logged
                class Example {
                  double x;
                }
              }
            }
          }
        }
        """;

    String expectedRootLogger =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;
        import java.lang.invoke.MethodHandles;
        import java.lang.invoke.VarHandle;

        public class A$B$C$D$ExampleLogger extends ClassSpecificLogger<A.B.C.D.Example> {
          private static final VarHandle $x;

          static {
            try {
              var lookup = MethodHandles.privateLookupIn(A.B.C.D.Example.class, MethodHandles.lookup());
              $x = lookup.findVarHandle(A.B.C.D.Example.class, "x", double.class);
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
            }
          }

          public A$B$C$D$ExampleLogger() {
            super(A.B.C.D.Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, A.B.C.D.Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              backend.log("x", ((double) $x.get(object)));
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void renamedInnerClass() {
    String source =
        """
        package edu.wpi.first.epilogue;

        class Outer {
          @Logged(name = "Custom Example") // For the sake of testing, needs "Example" somewhere in the name
          class Example {
            double x;
          }
        }
        """;

    String expectedRootLogger =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;
        import java.lang.invoke.MethodHandles;
        import java.lang.invoke.VarHandle;

        public class CustomExampleLogger extends ClassSpecificLogger<Outer.Example> {
          private static final VarHandle $x;

          static {
            try {
              var lookup = MethodHandles.privateLookupIn(Outer.Example.class, MethodHandles.lookup());
              $x = lookup.findVarHandle(Outer.Example.class, "x", double.class);
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
            }
          }

          public CustomExampleLogger() {
            super(Outer.Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, Outer.Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              backend.log("x", ((double) $x.get(object)));
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void diamondInheritance() {
    String source =
        """
        package edu.wpi.first.epilogue;

        @Logged
        interface I {}

        @Logged
        interface ExtendingInterface extends I {}

        @Logged
        class Base implements I {}

        /* Not @Logged */
        // Diamond inheritance from I (I -> ExtendingInterface -> Inheritor, I -> Base -> Inheritor)
        class Inheritor extends Base implements ExtendingInterface {}

        @Logged
        class Example {
          // If this is set to an `Inheritor` instance, it will be logged as a `Base` object rather
          // than `ExtendingInterface` or `I`
          I theField;
        }
        """;

    String expectedRootLogger =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;
        import java.lang.invoke.MethodHandles;
        import java.lang.invoke.VarHandle;

        public class ExampleLogger extends ClassSpecificLogger<Example> {
          private static final VarHandle $theField;

          static {
            try {
              var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
              $theField = lookup.findVarHandle(Example.class, "theField", edu.wpi.first.epilogue.I.class);
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
            }
          }

          public ExampleLogger() {
            super(Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              var $$theField = ((edu.wpi.first.epilogue.I) $theField.get(object));
              if ($$theField instanceof edu.wpi.first.epilogue.Base edu_wpi_first_epilogue_Base) {
                Epilogue.baseLogger.tryUpdate(backend.getNested("theField"), edu_wpi_first_epilogue_Base, Epilogue.getConfig().errorHandler);
              } else if ($$theField instanceof edu.wpi.first.epilogue.ExtendingInterface edu_wpi_first_epilogue_ExtendingInterface) {
                Epilogue.extendingInterfaceLogger.tryUpdate(backend.getNested("theField"), edu_wpi_first_epilogue_ExtendingInterface, Epilogue.getConfig().errorHandler);
              } else {
                // Base type edu.wpi.first.epilogue.I
                Epilogue.iLogger.tryUpdate(backend.getNested("theField"), $$theField, Epilogue.getConfig().errorHandler);
              };
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void instanceofChainWithField() {
    String source =
        """
        package edu.wpi.first.epilogue;

        @Logged
        interface I {}

        @Logged
        class Base implements I {}

        @Logged
        class Example {
          private I theField;
        }
        """;

    String expectedRootLogger =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;
        import java.lang.invoke.MethodHandles;
        import java.lang.invoke.VarHandle;

        public class ExampleLogger extends ClassSpecificLogger<Example> {
          private static final VarHandle $theField;

          static {
            try {
              var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
              $theField = lookup.findVarHandle(Example.class, "theField", edu.wpi.first.epilogue.I.class);
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
            }
          }

          public ExampleLogger() {
            super(Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              var $$theField = ((edu.wpi.first.epilogue.I) $theField.get(object));
              if ($$theField instanceof edu.wpi.first.epilogue.Base edu_wpi_first_epilogue_Base) {
                Epilogue.baseLogger.tryUpdate(backend.getNested("theField"), edu_wpi_first_epilogue_Base, Epilogue.getConfig().errorHandler);
              } else {
                // Base type edu.wpi.first.epilogue.I
                Epilogue.iLogger.tryUpdate(backend.getNested("theField"), $$theField, Epilogue.getConfig().errorHandler);
              };
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void nestedOptIn() {
    String source =
        """
        package edu.wpi.first.epilogue;

        class Implicit {
          @Logged double x;
        }

        class Example {
          @Logged Implicit i;
        }
        """;

    String expectedRootLogger =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;
        import java.lang.invoke.MethodHandles;
        import java.lang.invoke.VarHandle;

        public class ExampleLogger extends ClassSpecificLogger<Example> {
          private static final VarHandle $i;

          static {
            try {
              var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
              $i = lookup.findVarHandle(Example.class, "i", edu.wpi.first.epilogue.Implicit.class);
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
            }
          }

          public ExampleLogger() {
            super(Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              Epilogue.implicitLogger.tryUpdate(backend.getNested("i"), ((edu.wpi.first.epilogue.Implicit) $i.get(object)), Epilogue.getConfig().errorHandler);
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void customLogger() {
    String source =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.logging.*;

        record Point(int x, int y) {}

        @CustomLoggerFor(Point.class)
        class CustomPointLogger extends ClassSpecificLogger<Point> {
          public CustomPointLogger() {
            super(Point.class);
          }

          @Override
          public void update(EpilogueBackend backend, Point point) {
            // Implementation is irrelevant
          }
        }

        @Logged
        class Example {
          Point point;
        }
        """;

    String expectedGeneratedSource =
        """
      package edu.wpi.first.epilogue;

      import edu.wpi.first.epilogue.Logged;
      import edu.wpi.first.epilogue.Epilogue;
      import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
      import edu.wpi.first.epilogue.logging.EpilogueBackend;
      import java.lang.invoke.MethodHandles;
      import java.lang.invoke.VarHandle;

      public class ExampleLogger extends ClassSpecificLogger<Example> {
        private static final VarHandle $point;

        static {
          try {
            var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
            $point = lookup.findVarHandle(Example.class, "point", edu.wpi.first.epilogue.Point.class);
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
          }
        }

        public ExampleLogger() {
          super(Example.class);
        }

        @Override
        public void update(EpilogueBackend backend, Example object) {
          if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
            Epilogue.customPointLogger.tryUpdate(backend.getNested("point"), ((edu.wpi.first.epilogue.Point) $point.get(object)), Epilogue.getConfig().errorHandler);
          }
        }
      }
      """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void customGenericLogger() {
    String source =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.logging.*;
        import edu.wpi.first.math.numbers.*;
        import edu.wpi.first.math.Num;
        import edu.wpi.first.math.Vector;

        @CustomLoggerFor(Vector.class)
        class VectorLogger extends ClassSpecificLogger<Vector<?>> {
          public VectorLogger() {
            super((Class) Vector.class);
          }

          @Override
          public void update(EpilogueBackend backend, Vector<?> object) {
            // Implementation is irrelevant
          }
        }

        @Logged
        class Example {
          Vector<N3> vec;
        }
        """;

    String expectedGeneratedSource =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;
        import java.lang.invoke.MethodHandles;
        import java.lang.invoke.VarHandle;

        public class ExampleLogger extends ClassSpecificLogger<Example> {
          private static final VarHandle $vec;

          static {
            try {
              var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
              $vec = lookup.findVarHandle(Example.class, "vec", edu.wpi.first.math.Vector.class);
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
            }
          }

          public ExampleLogger() {
            super(Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              Epilogue.vectorLogger.tryUpdate(backend.getNested("vec"), ((edu.wpi.first.math.Vector<edu.wpi.first.math.numbers.N3>) $vec.get(object)), Epilogue.getConfig().errorHandler);
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedGeneratedSource);
  }

  @Test
  void genericLoggerForGenericType() {
    String source =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.logging.*;

        class Generic<T> { }

        @CustomLoggerFor(Generic.class)
        // Invalid: loggers cannot take type arguments
        class GenericLogger<T> extends ClassSpecificLogger<Generic<T>> {
          public GenericLogger() {
            super((Class) Generic.class);
          }

          @Override
          public void update(EpilogueBackend backend, Generic<T> object) {
            // Implementation is irrelevant
          }
        }

        @Logged
        class Example {
          Generic<String> genericField;
        }
        """;

    Compilation compilation =
        javac()
            .withOptions(kJavaVersionOptions)
            .withProcessors(new AnnotationProcessor())
            .compile(JavaFileObjects.forSourceString("edu.wpi.first.epilogue.Example", source));

    assertThat(compilation).failed();
    assertThat(compilation).hadErrorCount(1);

    assertCompilationError(
        "[EPILOGUE] Custom logger classes cannot take generic type arguments",
        9,
        1,
        compilation.errors().get(0));
  }

  @Test
  void warnsAboutNonLoggableFields() {
    String source =
        """
        package edu.wpi.first.epilogue;

        @Logged(warnForNonLoggableTypes = true)
        class Example {
          Throwable t;
        }
        """;

    Compilation compilation =
        javac()
            .withOptions(kJavaVersionOptions)
            .withProcessors(new AnnotationProcessor())
            .compile(JavaFileObjects.forSourceString("edu.wpi.first.epilogue.Example", source));

    assertThat(compilation).succeeded();
    assertEquals(1, compilation.notes().size());
    var warning = compilation.notes().get(0);
    var message = warning.getMessage(Locale.getDefault());
    assertEquals(
        "[EPILOGUE] Excluded from logs because java.lang.Throwable is not a loggable data type",
        message);
  }

  @Test
  void loggingRecords() {
    String source =
        """
        package edu.wpi.first.epilogue;

        @Logged
        record Example(double x, double y) { }
        """;

    String expectedRootLogger =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;

        public class ExampleLogger extends ClassSpecificLogger<Example> {
          public ExampleLogger() {
            super(Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              backend.log("x", object.x());
              backend.log("y", object.y());
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void errorsOnFieldNameConflicts() {
    String source =
        """
        package edu.wpi.first.epilogue;

        @Logged
        class Example {
          @Logged(name = "Custom Name") double x;
          @Logged(name = "Custom Name") double y;
          @Logged(name = "Custom Name") double z;
        }
        """;

    Compilation compilation =
        javac()
            .withProcessors(new AnnotationProcessor())
            .compile(JavaFileObjects.forSourceString("edu.wpi.first.epilogue.Example", source));

    assertThat(compilation).failed();
    assertThat(compilation).hadErrorCount(3);

    List<Diagnostic<? extends JavaFileObject>> errors = compilation.errors();
    assertAll(
        () ->
            assertCompilationError(
                "[EPILOGUE] Conflicting name detected: \"Custom Name\" is also used by Example.y, Example.z",
                5,
                40,
                errors.get(0)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Conflicting name detected: \"Custom Name\" is also used by Example.x, Example.z",
                6,
                40,
                errors.get(1)),
        () ->
            assertCompilationError(
                "[EPILOGUE] Conflicting name detected: \"Custom Name\" is also used by Example.x, Example.y",
                7,
                40,
                errors.get(2)));
  }

  @Test
  void doesNotErrorOnGetterMethod() {
    String source =
        """
        package edu.wpi.first.epilogue;

        @Logged
        class Example {
          double x;
          public double x() { return x; }
          public double getX() { return x; }
          public double aTotallyArbitraryNameForAnAccessorMethod() { return x; }
          public double withANoOpTransform() { return x + 0; }
          public double withTemp() { var temp = x; return temp; }
        }
        """;

    String expectedRootLogger =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;
        import java.lang.invoke.MethodHandles;
        import java.lang.invoke.VarHandle;

        public class ExampleLogger extends ClassSpecificLogger<Example> {
          private static final VarHandle $x;

          static {
            try {
              var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
              $x = lookup.findVarHandle(Example.class, "x", double.class);
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
            }
          }

          public ExampleLogger() {
            super(Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              backend.log("x", ((double) $x.get(object)));
              backend.log("withANoOpTransform", object.withANoOpTransform());
              backend.log("withTemp", object.withTemp());
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void configuredDefaultNaming() {
    String source =
        """
        package edu.wpi.first.epilogue;

        @Logged(defaultNaming = Logged.Naming.USE_HUMAN_NAME)
        class Example {
          double m_memberPrefix;
          double kConstantPrefix;
          double k_otherConstantPrefix;
          double s_otherPrefix;

          public double getTheGetterMethod() {
            return 0;
          }

          @Logged(defaultNaming = Logged.Naming.USE_CODE_NAME)
          public double optedOut() {
            return 0;
          }
        }
        """;

    String expectedRootLogger =
        """
        package edu.wpi.first.epilogue;

        import edu.wpi.first.epilogue.Logged;
        import edu.wpi.first.epilogue.Epilogue;
        import edu.wpi.first.epilogue.logging.ClassSpecificLogger;
        import edu.wpi.first.epilogue.logging.EpilogueBackend;
        import java.lang.invoke.MethodHandles;
        import java.lang.invoke.VarHandle;

        public class ExampleLogger extends ClassSpecificLogger<Example> {
          private static final VarHandle $m_memberPrefix;
          private static final VarHandle $kConstantPrefix;
          private static final VarHandle $k_otherConstantPrefix;
          private static final VarHandle $s_otherPrefix;

          static {
            try {
              var lookup = MethodHandles.privateLookupIn(Example.class, MethodHandles.lookup());
              $m_memberPrefix = lookup.findVarHandle(Example.class, "m_memberPrefix", double.class);
              $kConstantPrefix = lookup.findVarHandle(Example.class, "kConstantPrefix", double.class);
              $k_otherConstantPrefix = lookup.findVarHandle(Example.class, "k_otherConstantPrefix", double.class);
              $s_otherPrefix = lookup.findVarHandle(Example.class, "s_otherPrefix", double.class);
            } catch (ReflectiveOperationException e) {
              throw new RuntimeException("[EPILOGUE] Could not load private fields for logging!", e);
            }
          }

          public ExampleLogger() {
            super(Example.class);
          }

          @Override
          public void update(EpilogueBackend backend, Example object) {
            if (Epilogue.shouldLog(Logged.Importance.DEBUG)) {
              backend.log("Member Prefix", ((double) $m_memberPrefix.get(object)));
              backend.log("Constant Prefix", ((double) $kConstantPrefix.get(object)));
              backend.log("Other Constant Prefix", ((double) $k_otherConstantPrefix.get(object)));
              backend.log("Other Prefix", ((double) $s_otherPrefix.get(object)));
              backend.log("The Getter Method", object.getTheGetterMethod());
              backend.log("optedOut", object.optedOut());
            }
          }
        }
        """;

    assertLoggerGenerates(source, expectedRootLogger);
  }

  @Test
  void doesNotBreakWithPackageInfo() {
    String source =
        """
        package example;

        import edu.wpi.first.epilogue.*;

        @Logged
        class Example {}
        """;

    String packageInfo = """
        package example;
        """;

    Compilation compilation =
        javac()
            .withOptions(kJavaVersionOptions)
            .withProcessors(new AnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceString("example.Example", source),
                JavaFileObjects.forSourceString("example.package-info", packageInfo));

    assertThat(compilation).succeeded();
    compilation.generatedSourceFiles().stream()
        .filter(jfo -> jfo.getName().contains("Example"))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Logger file was not generated!"));
  }

  private void assertCompilationError(
      String message, long lineNumber, long col, Diagnostic<? extends JavaFileObject> diagnostic) {
    assertAll(
        () -> assertEquals(Diagnostic.Kind.ERROR, diagnostic.getKind(), "not an error"),
        () ->
            assertEquals(
                message, diagnostic.getMessage(Locale.getDefault()), "error message mismatch"),
        () -> assertEquals(lineNumber, diagnostic.getLineNumber(), "line number mismatch"),
        () -> assertEquals(col, diagnostic.getColumnNumber(), "column number mismatch"));
  }

  private void assertLoggerGenerates(String loggedClassContent, String loggerClassContent) {
    Compilation compilation =
        javac()
            .withOptions(kJavaVersionOptions)
            .withProcessors(new AnnotationProcessor())
            .compile(
                JavaFileObjects.forSourceString(
                    "edu.wpi.first.epilogue.Example", loggedClassContent));

    assertThat(compilation).succeeded();
    var generatedFiles = compilation.generatedSourceFiles();
    var generatedFile =
        generatedFiles.stream()
            .filter(jfo -> jfo.getName().contains("Example"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Logger file was not generated!"));
    try {
      var content = generatedFile.getCharContent(false);
      assertEquals(
          loggerClassContent.replace("\r\n", "\n"), content.toString().replace("\r\n", "\n"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
