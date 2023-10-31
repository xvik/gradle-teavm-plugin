package example;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipJVM;
import org.teavm.junit.TeaVMTestRunner;

/**
 * @author Vyacheslav Rusakov
 * @since 07.03.2023
 */
@RunWith(TeaVMTestRunner.class)
@SkipJVM
public class SampleTest {

    @Test
    public void simpleFields() {
        var x = 2;
        var y = 3;
        Assert.assertEquals(5, x + y);
    }
}
