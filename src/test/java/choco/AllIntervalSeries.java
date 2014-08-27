/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package choco;

import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;
import samples.AbstractProblem;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.IntConstraintFactory;
import solver.search.strategy.IntStrategyFactory;
import solver.variables.IntVar;
import solver.variables.VariableFactory;
import util.tools.StringUtils;

/**
 * CSPLib prob007:<br/>
 * "Given n in N, find a vector s = (s_1, ..., s_n), such that
 * <ul>
 * <li>s is a permutation of Z_n = {0,1,...,n-1};</li>
 * <li>the interval vector v = (|s_2-s_1|, |s_3-s_2|, ... |s_n-s_{n-1}|) is a permutation of Z_n-{0} = {1,2,...,n-1}.</li>
 * </ul>
 * <br/>
 * A vector v satisfying these conditions is called an all-interval series of size n;
 * the problem of finding such a series is the all-interval series problem of size n."
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 02/08/11
 */
public class AllIntervalSeries extends AbstractProblem {
    @Option(name = "-o", usage = "All interval series size.", required = false)
    private int m = 1000;

    @Option(name = "-v", usage = " use views instead of constraints.", required = false)
    private boolean use_views = false;

    IntVar[] vars;
    IntVar[] dist;

    Constraint[] ALLDIFF;
    Constraint[] OTHERS;

    @Override
    public void createSolver() {
        solver = new Solver("AllIntervalSeries");
    }

    @Override
    public void buildModel() {
        vars = VariableFactory.enumeratedArray("v", m, 0, m - 1, solver);
        dist = new IntVar[m - 1];

        if (!use_views) {
            dist = VariableFactory.enumeratedArray("dist", m - 1, 1, m - 1, solver);
            for (int i = 0; i < m - 1; i++) {
                solver.post(IntConstraintFactory.distance(vars[i + 1], vars[i], "=", dist[i]));
            }
        } else {
            for (int i = 0; i < m - 1; i++) {
				IntVar k = VariableFactory.bounded(StringUtils.randomName(),-20000,20000,solver);
				solver.post(IntConstraintFactory.sum(new IntVar[]{vars[i],k},vars[i+1]));
				dist[i] = VariableFactory.abs(k);
                solver.post(IntConstraintFactory.member(dist[i], 1, m - 1));
            }
        }

        ALLDIFF = new Constraint[2];
        ALLDIFF[0] = (IntConstraintFactory.alldifferent(vars, "BC"));
        ALLDIFF[1] = (IntConstraintFactory.alldifferent(dist, "BC"));
        solver.post(ALLDIFF);

        // break symetries
        OTHERS = new Constraint[2];
        OTHERS[0] = (IntConstraintFactory.arithm(vars[1], ">", vars[0]));
        OTHERS[1] = (IntConstraintFactory.arithm(dist[0], ">", dist[m - 2]));
        solver.post(OTHERS);
    }

    @Override
    public void configureSearch() {
        solver.set(IntStrategyFactory.minDom_LB(vars));
    }

    @Override
    public void solve() {
        solver.findSolution();
    }

    @Override
    public void prettyOut() {
        LoggerFactory.getLogger("bench").info("All interval series({})", m);
        StringBuilder st = new StringBuilder();
        st.append("\t");
        for (int i = 0; i < m - 1; i++) {
            st.append(String.format("%d <%d> ", vars[i].getValue(), dist[i].getValue()));
            if (i % 10 == 9) {
                st.append("\n\t");
            }
        }
        st.append(String.format("%d", vars[m - 1].getValue()));
        LoggerFactory.getLogger("bench").info(st.toString());
    }

    public static void main(String[] args) {
        new AllIntervalSeries().execute(args);
    }
}
