/**
 *  @test
 *  @bug 6822627
 *  @summary Test that ReferenceType.constantPool does not produce an NPE
 *
 *  @author Egor Ushakov
 *
 *  @run build TestScaffold VMConnection
 *  @run compile -g ConstantPoolInfoGC.java
 *  @run main ConstantPoolInfoGC
 */

import com.sun.jdi.ReferenceType;
import com.sun.tools.jdi.ReferenceTypeImpl;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Arrays;

    /********** target program **********/

class ConstantPoolGCTarg {
    public static void main(String[] args){
        System.out.println("Anything");
    }
}

    /********** test program **********/

public class ConstantPoolInfoGC extends TestScaffold {
    ReferenceType targetClass;

    ConstantPoolInfoGC(String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new ConstantPoolInfoGC(args).startTests();
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        targetClass = startToMain("ConstantPoolGCTarg").location().declaringType();

        if (vm().canGetConstantPool()) {
            byte[] cpbytes = targetClass.constantPool();

            // imitate SoftReference cleared
            Field constantPoolBytesRef = ReferenceTypeImpl.class.getDeclaredField("constantPoolBytesRef");
            constantPoolBytesRef.setAccessible(true);
            Reference softRef = (Reference) constantPoolBytesRef.get(targetClass);
            softRef.clear();

            byte[] cpbytes2 = targetClass.constantPool();
            if (!Arrays.equals(cpbytes, cpbytes2)) {
                failure("Consequent constantPool results vary, first was : " + cpbytes + ", now: " + cpbytes2);
            };

        } else {
            System.out.println("can get constant pool version not supported");
        }


        /*
         * resume until end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("ConstantPoolInfoGC: passed");
        } else {
            throw new Exception("ConstantPoolInfoGC: failed");
        }
    }
}
