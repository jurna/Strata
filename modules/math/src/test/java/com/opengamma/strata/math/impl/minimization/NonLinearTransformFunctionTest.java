/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.testng.Assert.assertEquals;

import java.util.BitSet;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.minimization.ParameterLimitsTransform.LimitType;

/**
 * Test.
 */
@Test
public class NonLinearTransformFunctionTest {

  private static final ParameterLimitsTransform[] NULL_TRANSFORMS;
  private static final ParameterLimitsTransform[] TRANSFORMS;

  private static final Function1D<DoubleMatrix1D, DoubleMatrix1D> FUNCTION = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {
    @Override
    public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
      ArgChecker.isTrue(x.size() == 2);
      double x1 = x.get(0);
      double x2 = x.get(1);
      return DoubleMatrix1D.of(
          Math.sin(x1) * Math.cos(x2),
          Math.sin(x1) * Math.sin(x2),
          Math.cos(x1));
    }
  };

  private static final Function1D<DoubleMatrix1D, DoubleMatrix2D> JACOBIAN = new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {
    @Override
    public DoubleMatrix2D evaluate(DoubleMatrix1D x) {
      ArgChecker.isTrue(x.size() == 2);
      double x1 = x.get(0);
      double x2 = x.get(1);
      double[][] y = new double[3][2];
      y[0][0] = Math.cos(x1) * Math.cos(x2);
      y[0][1] = -Math.sin(x1) * Math.sin(x2);
      y[1][0] = Math.cos(x1) * Math.sin(x2);
      y[1][1] = Math.sin(x1) * Math.cos(x2);
      y[2][0] = -Math.sin(x1);
      y[2][1] = 0;
      return new DoubleMatrix2D(y);
    }
  };

  static {
    NULL_TRANSFORMS = new ParameterLimitsTransform[2];
    NULL_TRANSFORMS[0] = new NullTransform();
    NULL_TRANSFORMS[1] = new NullTransform();

    TRANSFORMS = new ParameterLimitsTransform[2];
    TRANSFORMS[0] = new DoubleRangeLimitTransform(0, Math.PI);
    TRANSFORMS[1] = new SingleRangeLimitTransform(0, LimitType.GREATER_THAN);
  }

  @Test
  public void testNullTransform() {
    BitSet fixed = new BitSet();
    fixed.set(0);
    DoubleMatrix1D start = DoubleMatrix1D.of(Math.PI / 4, 1);
    UncoupledParameterTransforms transforms = new UncoupledParameterTransforms(start, NULL_TRANSFORMS, fixed);
    NonLinearTransformFunction transFunc = new NonLinearTransformFunction(FUNCTION, JACOBIAN, transforms);
    Function1D<DoubleMatrix1D, DoubleMatrix1D> func = transFunc.getFittingFunction();
    Function1D<DoubleMatrix1D, DoubleMatrix2D> jacFunc = transFunc.getFittingJacobian();

    DoubleMatrix1D x = DoubleMatrix1D.of(0.5);
    final double rootHalf = Math.sqrt(0.5);
    DoubleMatrix1D y = func.evaluate(x);
    assertEquals(3, y.size());
    assertEquals(rootHalf * Math.cos(0.5), y.get(0), 1e-9);
    assertEquals(rootHalf * Math.sin(0.5), y.get(1), 1e-9);
    assertEquals(rootHalf, y.get(2), 1e-9);

    DoubleMatrix2D jac = jacFunc.evaluate(x);
    assertEquals(3, jac.rowCount());
    assertEquals(1, jac.columnCount());
    assertEquals(-rootHalf * Math.sin(0.5), jac.get(0, 0), 1e-9);
    assertEquals(rootHalf * Math.cos(0.5), jac.get(1, 0), 1e-9);
    assertEquals(0, jac.get(2, 0), 1e-9);
  }

  @Test
  public void testNonLinearTransform() {
    BitSet fixed = new BitSet();
    DoubleMatrix1D start = DoubleMatrix1D.filled(2);
    UncoupledParameterTransforms transforms = new UncoupledParameterTransforms(start, TRANSFORMS, fixed);
    NonLinearTransformFunction transFunc = new NonLinearTransformFunction(FUNCTION, JACOBIAN, transforms);
    Function1D<DoubleMatrix1D, DoubleMatrix1D> func = transFunc.getFittingFunction();
    Function1D<DoubleMatrix1D, DoubleMatrix2D> jacFunc = transFunc.getFittingJacobian();

    VectorFieldFirstOrderDifferentiator diff = new VectorFieldFirstOrderDifferentiator();
    Function1D<DoubleMatrix1D, DoubleMatrix2D> jacFuncFD = diff.differentiate(func);

    DoubleMatrix1D testPoint = DoubleMatrix1D.of(4.5, -2.1);
    DoubleMatrix2D jac = jacFunc.evaluate(testPoint);
    DoubleMatrix2D jacFD = jacFuncFD.evaluate(testPoint);
    assertEquals(3, jac.rowCount());
    assertEquals(2, jac.columnCount());

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 2; j++) {
        assertEquals(jacFD.get(i, j), jac.get(i, j), 1e-6);
      }
    }
  }
}