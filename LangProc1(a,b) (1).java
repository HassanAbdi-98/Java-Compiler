public IRProgram compile(Program n) {
        List<IRExp> a = new ArrayList<>();
        List<IRStm> STM = new ArrayList<>();
        int i = 0;
        while(i<n.pd.fs.size()){
            IRExp exp = BINOP(TEMP("FP"),IROp.SUB,CONST(i+1));
            a.add(MEM(exp));
            i++;
        }
        i=0;
        String id = n.pd.id;
        IRExp exp = CALL(NAME(id),a);
        STM.add(EXP(exp));
        STM.add(JUMP(NAME("_END")));
        while(i<n.mds.size()){
            MethodDecl md = n.mds.get(i);
            STM.addAll(md.accept(stmCompiler));
            i++;
        }
        i=0;
        List<IRStm> stmlist = n.pd.accept(stmCompiler);
        while(i<stmlist.size()){
             STM.add(stmlist.get(i));
             i++;
        }



        return new IRProgram(stringMap,STM);
    }
private class StmCompiler extends VisitorAdapter<List<IRStm>> {
    @Override //?
        public List<IRStm> visit(ProcDecl n) {
            List<IRStm> stms = new ArrayList<>();
            stms.add(LABEL(n.id));
            stms.add(PROLOGUE(n.fs.size(), n.stackAllocation));
            for (Stm stm: n.ss) {
                stms.addAll(stm.accept(stmCompiler));
            }
            stms.add(EPILOGUE(n.fs.size(), n.stackAllocation));

            return stms;
        }

        @Override //?
        public List<IRStm> visit(FunDecl n) {
            List<IRStm> stms = new ArrayList<>();
            stms.add(LABEL(n.id));
            stms.add(PROLOGUE(n.fs.size(), n.stackAllocation));
            for (Stm stm: n.ss) {
                stms.addAll(stm.accept(stmCompiler));
            }
            stms.add(MOVE(TEMP("RV"), n.e.accept(expCompiler)));
            stms.add(EPILOGUE(n.fs.size(), n.stackAllocation));

            return stms;
        }

        @Override
        public List<IRStm> visit(StmCall n) {
            List<IRStm> stms = new ArrayList<>();
            List<IRExp> args = new ArrayList<>();
            for (int i =0; i < n.es.size(); i++){
                args.add(n.es.get(i).accept(expCompiler));
            }

            stms.add(EXP(CALL(NAME(n.id), args)));

            return stms;
        }

        @Override
        public List<IRStm> visit(StmOutchar s) {
            List<IRStm> stms = new ArrayList<>();
            stms.add(EXP(CALL(NAME("_printchar"), s.e.accept(expCompiler))));
            return stms;
        }

        @Override
        public List<IRStm> visit(StmVarDecl n) {
            return new ArrayList<>();
        }

        @Override
        public List<IRStm> visit(StmAssign n) {
            List<IRStm> stms = new ArrayList<>();
            stms.add(MOVE(MEM(BINOP(TEMP("FP"), IROp.ADD, CONST(n.v.offset))), n.e.accept(expCompiler)));
            return stms;
        }

        @Override
        public List<IRStm> visit(StmOutput n) {
            List<IRStm> stms = new ArrayList<>();
            stms.add(EXP(CALL(NAME("_printint"), n.e.accept(expCompiler))));
            return stms;
        }

        @Override
        public List<IRStm> visit(StmBlock n) {
            List<IRStm> stms = new ArrayList<>();
            for (int i = 0; i < n.ss.size(); i++) {
                stms.addAll(n.ss.get(i).accept(stmCompiler));
            }
            return stms;
        }

        @Override
        public List<IRStm> visit(StmIf n) { //imenaaa
            List<IRStm> stms = new ArrayList<>();
            IRExp exp = n.e.accept(expCompiler);

            IRStm st = n.st.accept(stmCompiler).get(0); //?
            IRStm sf = n.sf.accept(stmCompiler).get(0); //?

            String tr = FreshNameGenerator.makeName();
            String fl = FreshNameGenerator.makeName();
            String end = FreshNameGenerator.makeName();

            stms.add(CJUMP(exp, IROp.EQ, CONST(1), tr, fl));
            stms.add(LABEL(tr));

            stms.add(st);
            stms.add(JUMP(NAME(end)));
            stms.add(LABEL(fl));
            stms.add(sf);
            stms.add(LABEL(end));

            return stms;
        }

        @Override
        public List<IRStm> visit(StmWhile n) { //imenaaa
            List<IRStm> stms = new ArrayList<>();
            IRExp exp = n.e.accept(expCompiler);

            IRStm st = n.body.accept(stmCompiler).get(0);
            String main1 = FreshNameGenerator.makeName();
            String main0 = FreshNameGenerator.makeName();
            String main = FreshNameGenerator.makeName();


            stms.add(LABEL(main));
            stms.add(CJUMP(exp, IROp.EQ, CONST(1), main1, main0));
            stms.add(LABEL(main1));
            stms.add(st);
            stms.add(CJUMP(exp, IROp.EQ, CONST(1), main, main0));
            stms.add(LABEL(main0));
            return stms;
        }
        }

        private class ExpCompiler extends VisitorAdapter<IRExp> {

        @Override
        public IRExp visit(ExpInteger e) {

            return CONST(e.i);
        }

        @Override
        public IRExp visit(ExpTrue e) {

            return CONST(1);
        }

        @Override
        public IRExp visit(ExpFalse e) {

            return CONST(0);
        }

        @Override
        public IRExp visit(ExpVar n) {

            return MEM(BINOP(TEMP("FP"),IROp.ADD,CONST(n.v.offset)));

        }

        @Override //?
        public IRExp visit(ExpNot n) {
            if (n.e.accept(expCompiler) == (CONST(1))){
                return CONST(0);
            }
            else {
                return CONST(1);
            }
        }

        @Override //?
        public IRExp visit(ExpOp n) {

            return BINOP(n.e1.accept(expCompiler), convert(n.op), n.e2.accept(expCompiler));
        }

        public IROp convert(ExpOp.Op op){
            if (op.equals(ExpOp.Op.PLUS)){
                return IROp.ADD;
            }
            else if (op.equals(ExpOp.Op.MINUS)){
                return IROp.SUB;
            }
            else if (op.equals(ExpOp.Op.TIMES)){
                return IROp.MUL;
            }
            else if (op.equals(ExpOp.Op.DIV)){
                return IROp.DIV;
            }
            else if (op.equals(ExpOp.Op.EQUALS)){
                return IROp.EQ;
            }

            else if (op.equals(ExpOp.Op.LESSTHAN)){
                return IROp.LT;
            }

            else{
                return IROp.LE;
            }
        }

        @Override //?
        public IRExp visit(ExpCall n) {
            List<IRExp> args = new ArrayList<>();
            for (int i = 0; i < n.es.size(); i++){
                args.add(n.es.get(i).accept(expCompiler));
            }

            return CALL(NAME(n.id), args);
        }
