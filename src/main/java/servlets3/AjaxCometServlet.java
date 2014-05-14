package servlets3;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Java source code come from an article on a blog of java.net. I just add some comments on it.
 * You can run this demo in Glassfish v3.
 */
@WebServlet(urlPatterns = {"/chat"}, asyncSupported = true)
public class AjaxCometServlet extends HttpServlet {
    private static final long serialVersionUID = -2919167206889576860L;

    private static final String BEGIN_SCRIPT_TAG = "<script type='text/javascript'>\n";
    private static final String END_SCRIPT_TAG = "</script>\n";

    private static final Queue<AsyncContext> queue = new ConcurrentLinkedQueue<AsyncContext>();
    private static final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<String>();
    private Thread notifierThread = null;
    // TODO set queues and messageQueues to map...
    private static final Map<String, Queue<AsyncContext>> rooms = new HashMap<String, Queue<AsyncContext>>();

    /**
     * Run a notifier thread. When a new message comes in, this thread will send a message to all of clients
     * in the queue.
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        Runnable notifierRunnable = new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    String message = null;
                    try {
                        message = messageQueue.take();
                        for (AsyncContext ac : queue) {
                            try {
                                PrintWriter writer = ac.getResponse().getWriter();
                                writer.println(message);
                                writer.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                                queue.remove(ac);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }
            }
        };
        notifierThread = new Thread(notifierRunnable);
        notifierThread.start();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        res.setHeader("Cache-Control", "private");
        // Pragma tell the client whether cache the page or not.
        res.setHeader("Pragma", "no-cache");
        PrintWriter writer = res.getWriter();
        writer.flush();

        final AsyncContext ac = req.startAsync();
        ac.setTimeout(10 * 60 * 1000);
        

        ac.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) throws IOException {
                queue.remove(ac);
            }

            @Override
            public void onTimeout(AsyncEvent event) throws IOException {
                queue.remove(ac);
            }

            /**
             * When you close the browser, this method will be invoked.
             */
            @Override
            public void onError(AsyncEvent event) throws IOException {
                queue.remove(ac);
            }

            @Override
            public void onStartAsync(AsyncEvent event) throws IOException {
            }
        });

        queue.add(ac);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setHeader("Cache-Control", "private");
        response.setHeader("Pragma", "no-cache");

        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");
        String name = request.getParameter("name");
        String room = request.getParameter("room");

        if ("login".equals(action)) {
            String message = BEGIN_SCRIPT_TAG + toJsonp("System Message", name + " has joined.") + END_SCRIPT_TAG;
            notify(room, message);
            response.getWriter().println("success");
        } else if ("post".equals(action)) {
            String message = request.getParameter("message");
            message = BEGIN_SCRIPT_TAG + toJsonp(name, message) + END_SCRIPT_TAG;
            notify(room, message);
            response.getWriter().println("success");
        } else {
            response.sendError(422, "Unprocessable Entity");
        }
    }

    @Override
    public void destroy() {
        queue.clear();
        notifierThread.interrupt();
    }

    /**
     * Put the message into the message queue.
     */
    private void notify(String room, String msg) throws IOException {
        try {
            messageQueue.put(msg);
        } catch (Exception ex) {
            IOException t = new IOException();
            t.initCause(ex);
            throw t;
        }
    }

    private String escape(String orig) {
        StringBuffer buffer = new StringBuffer(orig.length());

        for (int i = 0; i < orig.length(); i++) {
            char c = orig.charAt(i);
            switch (c) {
                case '\b':
                    buffer.append("\\b");
                    break;
                case '\f':
                    buffer.append("\\f");
                    break;
                case '\n':
                    buffer.append("<br />");
                    break;
                case '\r':
                    // ignore
                    break;
                case '\t':
                    buffer.append("\\t");
                    break;
                case '\'':
                    buffer.append("\\'");
                    break;
                case '\"':
                    buffer.append("\\\"");
                    break;
                case '\\':
                    buffer.append("\\\\");
                    break;
                case '<':
                    buffer.append("&lt;");
                    break;
                case '>':
                    buffer.append("&gt;");
                    break;
                case '&':
                    buffer.append("&amp;");
                    break;
                default:
                    buffer.append(c);
            }
        }

        return buffer.toString();
    }

    private String toJsonp(String name, String message) {
        // "window.parent.update" will invode the javascript method "update" on the target page.
        return "window.parent.update({name:\"" + escape(name) + "\",message:\"" + escape(message) + "\"});\n";
    }
}