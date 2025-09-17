import org.springframework.web.filter.ShallowEtagHeaderFilter;
import java.lang.reflect.Method;

public class temp_check {
    public static void main(String[] args) {
        Class<?> clazz = ShallowEtagHeaderFilter.class;
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().contains("generateETag") || method.getName().contains("ETag")) {
                System.out.println(method.toString());
            }
        }
    }
}
