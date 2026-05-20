/*
 * The baseCode project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.util.math;

/**
 * Numeric constants used in some ports of statistical code.
 * <p>
 * Pulled in-tree from baseCode {@code ubic.basecode.math.Constants} as part of
 * the baseCode math pilot retirement (see {@code BASECODE_DEP_AUDIT.md}).
 *
 * @author pavlidis
 */
public abstract class Constants {

    /* 1/pi */
    public static final double M_1_PI = 0.31830988618379067153776752674502872406891929148;
    public static final double M_1_SQRT_2 = 0.707106781186547524400844362105;
    /* 1/sqrt(2) */
    public static final double M_1_SQRT_2PI = 0.398942280401432677939946059934;

    public static final double M_LN_2 = 0.693147180559945309417232121458176568;
    /* log(sqrt(2*pi)) = log(2*pi)/2 : */
    public static final double M_LN_SQRT_2PI = 0.91893853320467274178032973640562;

    /* log(sqrt(pi)) = log(pi)/2 : */
    public static final double M_LN_SQRT_PI = 0.5723649429247000870717136756765293558;
    /* log(sqrt(pi/2)) = log(pi/2)/2 : */
    public static final double M_LN_SQRT_PId2 = 0.225791352644727432363097614947441;

    public static final double M_LOG10_2 = 0.301029995663981195213738894724493027;

    /* pi/2 */
    public static final double M_PI_2 = 1.57079632679489661923132169163975144209858469969;

    public static final double M_PI_4 = M_PI_2 / 2.0;

    public static final double M_PI_half = 1.570796326794896619231321691640;

    /* sqrt(2) */
    public static final double M_SQRT_2 = 1.4142135623730950488016887242097;
    public static final double M_SQRT_2dPI = 0.79788456080286535587989211986876;
    /* !* #endif /*4! */
    /* sqrt(pi), 1/sqrt(2pi), sqrt(2/pi) : */
    public static final double M_SQRT_PI = 1.772453850905516027298167483341;

    public static final double SMALL = 1e-10;
    public static final double SMALLISH = 1e-5;
    public static final double TINY = 1e-20;
}
