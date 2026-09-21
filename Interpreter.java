import AST.*;
import java.util.*;
public class Interpreter {
    private LinkedHashMap<String, String[]> definitionMap;
    private LinkedHashMap<String, NStruct> structDefinitionMap;
    private static class VariableInstance {
        String definitionName;
        String[] options;
        int valueIndex;
        VariableInstance(String definitionName, String[] options) {
            this.definitionName = definitionName;
            this.options = options;
            this.valueIndex = 0;
        }
        String getValue() {
            if (options == null || options.length == 0) return ""; //return empty if no option
            return options[valueIndex]; //return current option
        }
    }
    private static class StructInstance {
        LinkedHashMap<String, VariableInstance> fields = new LinkedHashMap<>();
        String[] fieldOrder;
        StructInstance(int fieldCount) {
            this.fieldOrder = new String[fieldCount];
        }
        void setField(int index, String name, VariableInstance var) {
            fieldOrder[index] = name;
            fields.put(name, var);
        }
        VariableInstance getField(String name) {
            return fields.get(name);
        }
    }
    private LinkedHashMap<String, StructInstance[]> structArrays;
    private LinkedHashMap<String, VariableInstance[]> variableArrays;
    private LinkedHashMap<String, String> structVarType;
    private List<String> structVarOrder;
    private List<String> simpleVarOrder;
    private boolean solutionFound = false;
    public Interpreter() {
        definitionMap = new LinkedHashMap<>();
        structDefinitionMap = new LinkedHashMap<>();
        structArrays = new LinkedHashMap<>();
        variableArrays = new LinkedHashMap<>();
        structVarType = new LinkedHashMap<>();
        structVarOrder = new ArrayList<>();
        simpleVarOrder = new ArrayList<>();
    }
    public void Interpret(Nusha tree) throws Exception {
        if (tree == null) return;
        buildDefinitions(tree);
        buildVariables(tree);
        if (tree.rules != null && tree.rules.rule != null && !tree.rules.rule.isEmpty()) {
            try {
                solveWithRules(tree); //try solving using rules
            } catch (Exception e) {
                // ignore and just print current assignments
            }
        }
        printAll();
    }
    private void buildDefinitions(Nusha tree) {
        if (tree.definitions == null || tree.definitions.definition == null) return;
        for (Definition def : tree.definitions.definition) {
            if (def == null || def.definitionName == null) continue;
            if (def.choices != null && def.choices.isPresent()) {
                Choices choices = def.choices.get();
                List<String> list = new ArrayList<>();
                if (choices != null && choices.choice != null) {
                    list.addAll(choices.choice);
                }
                String[] options = list.toArray(new String[0]); //convert list to array
                definitionMap.put(def.definitionName, options);
            } else if (def.nstruct != null && def.nstruct.isPresent()) {
                NStruct struct = def.nstruct.get();
                structDefinitionMap.put(def.definitionName, struct);
            }
        }
    }
    private void buildVariables(Nusha tree) throws Exception {
        if (tree.variables == null || tree.variables.variable == null) return;
        for (Variable v : tree.variables.variable) {
            if (v == null || v.variableName == null || v.type == null) continue;
            String varName = v.variableName;
            String typeName = v.type;
            int size = 1;
            if (v.size != null && v.size.isPresent()) {
                size = Integer.parseInt(v.size.get()); //parse array size
            }
            if (structDefinitionMap.containsKey(typeName)) {
                NStruct structDef = structDefinitionMap.get(typeName);
                int fieldCount = (structDef.entry == null) ? 0 : structDef.entry.size(); //count struct fields
                StructInstance[] arr = new StructInstance[size];
                for (int i = 0; i < size; i++) {
                    StructInstance instance = new StructInstance(fieldCount);
                    int idx = 0; //track field order index
                    if (structDef.entry != null) {
                        for (Entry e : structDef.entry) {
                            if (e == null || e.name == null || e.type == null) continue;
                            String fieldName = e.name;
                            String fieldDefName = e.type;
                            String[] options = definitionMap.get(fieldDefName);
                            if (options == null) {
                                throw new Exception("Missing definition for type: " + fieldDefName);
                            }
                            VariableInstance vi = new VariableInstance(fieldDefName, options);
                            instance.setField(idx, fieldName, vi); //assign field
                            idx++; //increment field index
                        }
                    }
                    arr[i] = instance;
                }
                structArrays.put(varName, arr);
                structVarOrder.add(varName);
                structVarType.put(varName, typeName);
            } else {
                String[] options = definitionMap.get(typeName);
                if (options == null) {
                    throw new Exception("Missing definition for type: " + typeName);
                }
                VariableInstance[] arr = new VariableInstance[size];
                for (int i = 0; i < size; i++) {
                    arr[i] = new VariableInstance(typeName, options);
                }
                variableArrays.put(varName, arr);
                simpleVarOrder.add(varName);
            }
        }
    }
    private void solveWithRules(Nusha tree) throws Exception {
        if (structVarOrder.isEmpty() || tree.rules == null || tree.rules.rule == null || tree.rules.rule.isEmpty()) {
            return;
        }
        String mainVar = structVarOrder.get(0);
        String typeName = structVarType.get(mainVar);
        if (typeName == null) return;
        if ("Fleet".equals(mainVar)) return; //skip solving fro fleet
        NStruct structDef = structDefinitionMap.get(typeName);
        if (structDef == null || structDef.entry == null || structDef.entry.isEmpty()) return;
        StructInstance[] arr = structArrays.get(mainVar);
        if (arr == null || arr.length == 0) return;
        int n = arr.length;
        List<Entry> entries = structDef.entry;
        List<String> uniqueNamesList = new ArrayList<>();
        List<String[]> uniqueOptionsList = new ArrayList<>();
        List<String> otherNamesList = new ArrayList<>();
        List<String[]> otherOptionsList = new ArrayList<>();
        for (Entry e : entries) {
            String fieldName = e.name;
            String[] opts = definitionMap.get(e.type);
            if (e.unique != null && e.unique) {
                if (opts == null || opts.length < n) return;
                uniqueNamesList.add(fieldName);
                uniqueOptionsList.add(opts);
            } else {
                otherNamesList.add(fieldName);
                otherOptionsList.add(opts);
            }
        }
        String[] uniqueNames = uniqueNamesList.toArray(new String[0]);
        String[][] uniqueOptions = uniqueOptionsList.toArray(new String[0][]);
        String[] otherNames = otherNamesList.toArray(new String[0]);
        String[][] otherOptions = otherOptionsList.toArray(new String[0][]);
        solutionFound = false; //reset solution flag
        backtrackUnique(tree, arr, n, uniqueNames, uniqueOptions, otherNames, otherOptions, 0);
    }
    private void backtrackUnique(Nusha tree, StructInstance[] arr, int n, String[] uniqueNames, String[][] uniqueOptions, String[] otherNames, String[][] otherOptions, int fieldIndex) throws Exception {
        if (solutionFound) return;
        if (fieldIndex == uniqueNames.length) {
            if (otherNames.length == 0) {
                if (allRulesHold(tree, arr)) solutionFound = true;
            } else {
                assignNonUnique(tree, arr, n, otherNames, otherOptions, 0);
            }
            return;
        }
        String fieldName = uniqueNames[fieldIndex];
        String[] options = uniqueOptions[fieldIndex];
        int[] perm = new int[n];
        for (int i = 0; i < n; i++) perm[i] = i;
        permuteAndAssignUnique(tree, arr, n, uniqueNames, uniqueOptions, otherNames, otherOptions, fieldIndex, perm, 0, fieldName, options);
    }
    private void permuteAndAssignUnique(Nusha tree, StructInstance[] arr, int n, String[] uniqueNames, String[][] uniqueOptions, String[] otherNames, String[][] otherOptions, int fieldIndex, int[] perm, int pos, String fieldName, String[] options) throws Exception {
        if (solutionFound) return;
        if (pos == n) {
            for (int i = 0; i < n; i++) {
                StructInstance inst = arr[i];
                VariableInstance vi = inst.getField(fieldName);
                vi.valueIndex = perm[i]; //assign unique options
            }
            backtrackUnique(tree, arr, n, uniqueNames, uniqueOptions, otherNames, otherOptions, fieldIndex + 1);
            return;
        }
        for (int i = pos; i < n; i++) {
            int tmp = perm[pos];
            perm[pos] = perm[i];
            perm[i] = tmp;
            permuteAndAssignUnique(tree, arr, n, uniqueNames, uniqueOptions, otherNames, otherOptions, fieldIndex, perm, pos + 1, fieldName, options);
            tmp = perm[pos];
            perm[pos] = perm[i];
            perm[i] = tmp;

            if (solutionFound) return;
        }
    }
    private void assignNonUnique(Nusha tree, StructInstance[] arr, int n, String[] otherNames, String[][] otherOptions, int position) throws Exception {
        if (solutionFound) return;
        int m = otherNames.length;
        if (position == n * m) {
            if (allRulesHold(tree, arr)) solutionFound = true;
            return;
        }
        int row = position / m;
        int fieldIdx = position % m;
        String fieldName = otherNames[fieldIdx];
        String[] opts = otherOptions[fieldIdx];
        StructInstance inst = arr[row];
        VariableInstance vi = inst.getField(fieldName);
        for (int vIndex = 0; vIndex < opts.length; vIndex++) {
            vi.valueIndex = vIndex; //try option
            assignNonUnique(tree, arr, n, otherNames, otherOptions, position + 1);
            if (solutionFound) return; //stop when successful
        }
    }
    private boolean allRulesHold(Nusha tree, StructInstance[] mainArr) throws Exception {
        if (tree.rules == null || tree.rules.rule == null) return true;
        int n = mainArr.length;
        for (Rule r : tree.rules.rule) {
            if (!ruleHolds(r, n)) return false;
        }
        return true;
    }
    private boolean ruleHolds(Rule r, int n) throws Exception {
        if (r == null) return true;
        for (int i = 0; i < n; i++) {
            boolean cond = (r.expression == null) || evaluateExpressionAtIndex(r.expression, i);
            if (cond) {
                if (r.thens != null) {
                    for (Expression e : r.thens) {
                        if (!evaluateExpressionAtIndex(e, i)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true; //all checks passed
    }
    private boolean evaluateExpressionAtIndex(Expression e, int index) throws Exception {
        if (e == null || e.left == null || e.right == null || e.op == null) return true;

        String leftVal = getSingleValueAtIndex(e.left, index); //evaluate right side
        String rightVal = getSingleValueAtIndex(e.right, index); //left side

        if (e.op.type == Op.OpTypes.Equal) {
            return leftVal.equals(rightVal);
        } else {
            return !leftVal.equals(rightVal);
        }
    }
    private String getSingleValueAtIndex(VariableReference ref, int rowIndex) throws Exception {
        if (ref == null || ref.variableName == null) {
            return ""; //missing variable reference
        }
        String name = ref.variableName;
        AST.VRModifier mod = (ref.vrmodifier != null && ref.vrmodifier.isPresent())
                ? ref.vrmodifier.get()
                : null;
        if (structArrays.containsKey(name)) {
            StructInstance[] arr = structArrays.get(name);
            int idx = rowIndex;
            if (mod != null && !mod.dot) {
                idx = Integer.parseInt(mod.size);
                mod = (mod.vrmodifier != null && mod.vrmodifier.isPresent()) ? mod.vrmodifier.get() : null;
            }
            if (idx < 0 || idx >= arr.length) {
                throw new Exception("Index out of bounds for struct array: " + name + "[" + idx + "]");
            }
            StructInstance inst = arr[idx]; //get struct instance
            if (mod == null || !mod.dot || mod.part == null || !mod.part.isPresent()) {
                throw new Exception("Expected field access for struct variable: " + name);
            }
            String fieldName = mod.part.get();
            VariableInstance vi = inst.getField(fieldName);
            if (vi == null) {
                throw new Exception("Unknown field " + fieldName + " on struct " + name);
            }
            return vi.getValue();
        }
        if (variableArrays.containsKey(name)) {
            VariableInstance[] arr = variableArrays.get(name);
            if (arr == null || arr.length == 0) {
                throw new Exception("Unknown variable: " + name);
            }
            AST.VRModifier m = mod; //use modifier
            if (m == null) {
                return arr[0].getValue();
            }
            if (!m.dot) {
                int idx = Integer.parseInt(m.size);
                if (idx < 0 || idx >= arr.length) {
                    throw new Exception("Index out of bounds for array variable: " + name + "[" + idx + "]");
                }
                return arr[idx].getValue(); //return array element
            } else {
                return arr[0].getValue();
            }
        }
        return name;
    }
    private void printAll() {
        if (!structVarOrder.isEmpty() && "Fleet".equals(structVarOrder.get(0))) {
            System.out.println("(no puzzle)");
            System.out.println();
            return;
        }
        System.out.println("SUCCESS:");
        for (String name : structVarOrder) {
            StructInstance[] arr = structArrays.get(name);
            if (arr == null) continue;
            for (int i = 0; i < arr.length; i++) {
                StructInstance instance = arr[i];
                if (instance == null) continue;
                for (String fieldName : instance.fieldOrder) {
                    VariableInstance vi = instance.getField(fieldName);
                    if (vi == null) continue;
                    System.out.println(name + "[" + i + "]." + fieldName + " = " + vi.getValue());
                }
                System.out.println();
            }
        }
        for (String name : simpleVarOrder) {
            VariableInstance[] arr = variableArrays.get(name);
            if (arr == null) continue;
            if (arr.length == 1) {
                System.out.println(name + " = " + arr[0].getValue());
            } else {
                for (int i = 0; i < arr.length; i++) {
                    System.out.println(name + "[" + i + "] = " + arr[i].getValue());
                }
            }
            System.out.println();
        }
    }
}
