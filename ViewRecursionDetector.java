package de.launsch.coremedia.cae.view.errorhandling;

import com.coremedia.objectserver.view.*;
import org.xml.sax.ContentHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;

/**
* winfried.mosler@launsch.de
* Licensed under CC0 Creative Commons Zero
*
* This decorator for the CoreMedia CAE view template resolving processes
* prevents endless recursions during the rendering.
* It maintains a stack of all objects with their respective views and
* stops if an object is rendered with the same view once again.
*
* If your templates rely on request parameters for dispatching another template
* you will need to restructure your template logic.
*
* If a recursion is detected, a ViewException is rendered instead, which should
* be easily readable in the Preview webapp but ignored in the Delivery webapp.
*/
public class ViewRecursionDetectorDecorator extends ViewDecoratorBase {

 private static final String REQUEST_ATTRIBUTE_VIEWSTACK = "viewstack";

 @Override
 protected Decorator getDecorator(View view) {
   return new ViewRecursionDetector();
 }

 private static class ViewRecursionDetector extends ViewDecoratorBase.Decorator {
   @Override
   public void decorate(ServletView decorated, Object bean, String view, HttpServletRequest request, HttpServletResponse response) {
     try {
       checkRecursion(request, bean, view);
       super.decorate(decorated, bean, view, request, response);
       clearCheckRecursion(request, bean, view);
     } catch (RecursionException e) {
       try {
         ViewUtils.render(new ViewException(null, e), null, response.getWriter(), request, response);
       } catch (IOException e1) {
         e1.printStackTrace();
       }
     }
   }

   @Override
   public void decorate(TextView decorated, Object bean, String view, Writer out, HttpServletRequest request, HttpServletResponse response) {
     try {
       checkRecursion(request, bean, view);
       super.decorate(decorated, bean, view, out, request, response);
       clearCheckRecursion(request, bean, view);
     } catch (RecursionException e) {
       ViewUtils.render(new ViewException(null, e), null, out, request, response);
     }
   }

   @Override
   public void decorate(XmlView decorated, Object bean, String view, ContentHandler out, HttpServletRequest request, HttpServletResponse response) {
     try {
       checkRecursion(request, bean, view);
       super.decorate(decorated, bean, view, out, request, response);
       clearCheckRecursion(request, bean, view);
     } catch (RecursionException e) {
       ViewUtils.render(new ViewException(null, e), null, out, request, response);
     }
   }


 /**
  * Maintains a stack of all called views.
  * Throws an Exception when a recursion is detected
  *
  * @param httpServletRequest
  * @param bean
  * @param view
  * @throws RecursionException to indicate a (malicious) recursion.
  */
 protected void checkRecursion(HttpServletRequest httpServletRequest, Object bean, String view) throws RecursionException {
   LinkedList<BeanAndView> stack = (LinkedList<BeanAndView>) httpServletRequest.getAttribute(REQUEST_ATTRIBUTE_VIEWSTACK);

   if (stack == null) {
     stack = new LinkedList<BeanAndView>();
     httpServletRequest.setAttribute(REQUEST_ATTRIBUTE_VIEWSTACK, stack);
   }

   final BeanAndView newBeanAndView = new BeanAndView(bean, view);
   if (stack.contains(newBeanAndView)) {
     throw new RecursionException("Recursion detected, bean " + bean + " with view " + view + " was included already.");
   } else {
     stack.add(newBeanAndView);
   }
 }

 /**
  * Call after rendering the view to clear the stack
  *
  * @param httpServletRequest
  * @param bean
  * @param view
  */
 protected void clearCheckRecursion(HttpServletRequest httpServletRequest, Object bean, String view) {
   LinkedList<BeanAndView> stack = (LinkedList<BeanAndView>) httpServletRequest.getAttribute(REQUEST_ATTRIBUTE_VIEWSTACK);

   if (stack != null) {
     final BeanAndView newBeanAndView = new BeanAndView(bean, view);
     stack.remove(newBeanAndView);
   }
 }
}


private static class BeanAndView {
 private final Object bean;
 private final String view;

 private BeanAndView(Object bean, String view) {
   this.bean = bean;
   this.view = view;
 }

 @Override
 public boolean equals(Object o) {
   if (this == o) {
     return true;
   }
   if (o == null || getClass() != o.getClass()) {
     return false;
   }

   BeanAndView that = (BeanAndView) o;

   if (bean != null ? !bean.equals(that.bean) : that.bean != null) {
     return false;
   }
   if (view != null ? !view.equals(that.view) : that.view != null) {
     return false;
   }

   return true;
 }

 @Override
 public int hashCode() {
   int result = bean != null ? bean.hashCode() : 0;
   result = 31 * result + (view != null ? view.hashCode() : 0);
   return result;
 }
}

public static class RecursionException extends Exception {

 public RecursionException(String s) {
   super(s);
 }
}
}
