package mpu;

import lombok.SneakyThrows;
import mpc.exception.CleanMessageRuntimeException;
import mpc.exception.ExecRuntimeException;
import mpc.fs.tmpfile.TmpFileOperation;
import mpc.fs.tmpfile.TmpFolderOperation;
import mpe.core.P;
import mpe.rt.SheduledThread;
import mpe.rt.core.ExecRq;
import mpu.core.ARG;
import mpu.core.RW;
import mpu.pare.Pare;
import mpu.str.JOIN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Алиас для System + функции для взамодействия с системой
 */
public class Sys {

	public static void main(String[] args) {
//		X.exit(execRq);
//		Sys.exec_UnsafeSpace("ssh HOST tail -100 /opt/tomcat-9/logs/catalina.out");
	}

	public static final Logger L = LoggerFactory.getLogger(Sys.class);
	public static final String NULL0 = "0";

	public static boolean isOsWindows() {
		return System.getProperty("os.name").startsWith("Windows");
	}


	/**
	 * *************************************************************
	 * --------------------------- PRINT -----------------------
	 * *************************************************************
	 */
	public static void p(Throwable s) {
		s.printStackTrace(System.out);
	}

	public static void p(Object s) {
		System.out.println(s);
	}

	public static void pf(String s, Object... args) {
		System.out.println(X.f(s, args));
	}

	public static void e(String s, Object... args) {
		e(String.format(s, args));
	}

	public static void e(String m) {
		System.err.println(m);
	}

	public static void e(Object o) {
		e(o == null ? null : o.toString());
	}

	/**
	 * *************************************************************
	 * ------------------------- EXEC ------------------------
	 * *************************************************************
	 */

	@SneakyThrows
	public static void exec_UnsafeSpace(String command) {
		Process exec = Runtime.getRuntime().exec(command);
		try {
			int i = exec.exitValue();
			Sys.p(i + ":" + command);
		} catch (Exception ex) {
			L.error("Exec error, command : " + command + " : " + ex.getMessage());
		}
	}

	@SneakyThrows
	public static void exec_SafeSpace(String... command) {
		Process exec = Runtime.getRuntime().exec(command);
		try {
			int i = exec.exitValue();
			Sys.p(i + ":" + command);
		} catch (Exception ex) {
			L.error("Exec error, command : " + command + " : " + ex.getMessage());
		}
	}

	@SneakyThrows
	public static ExecRq exec_rq(String... cmds) {
		return ExecRq.exec(false, cmds);
	}

	/**
	 * *************************************************************
	 * --------------------------- EXIT -----------------------
	 * *************************************************************
	 */

	public static void exit() {
		System.exit(0);
	}

	public static void exit(String message) {
		p(message);
		p("exit(0)/" + (message == null ? "-1" : message.length()));
		System.exit(0);
	}

	public static void exitf(String obj, Object... args) {
		pf(obj, args);
		p("exit(0)");
		System.exit(0);
	}

	public static void exit(Object... objs) {
		if (objs == null) {
			p(null);
			return;
		}
		for (Object obj : objs) {
			X.p(obj);
		}
		p("exit(0)/" + objs.length);
		System.exit(0);
	}

	public static void exit(Object obj) {
		P.pnice(obj);
		p("exit(0)");
		System.exit(0);
	}

	/**
	 * *************************************************************
	 * ------------------------- OPEN LINUX APP's ------------------------
	 * *************************************************************
	 */

	@SneakyThrows
	public static void open_Code(Path path) {
		exec_SafeSpace("code", path.toString());
	}

	@SneakyThrows
	public static void open_Browser(String url) {
		exec_SafeSpace("chromium", url);
	}

	@SneakyThrows
	public static void open_Nautilus(String file) {
		open_Nautilus(Paths.get(file));
	}

	@SneakyThrows
	public static void open_Nautilus(Path file) {
		exec_SafeSpace("nautilus", file.toString());
	}

	@SneakyThrows
	public static void open_Terminal(Path file) {
		exec_SafeSpace("gnome-terminal", file.toString());
	}

	/**
	 * *************************************************************
	 * -------------------------  SAY ------------------------
	 * *************************************************************
	 */

	public static void beep() {
		Toolkit.getDefaultToolkit().beep();
	}

	public static void saySheduled(String message, int at_time) {
		new SheduledThread(at_time) {
			@Override
			protected void doWork() {
				say((message));

			}
		};
	}

	public static void say(String msg, Object... args) {
		msg = X.f_(msg, args);
		if (isOsWindows()) {
			Sayer.sayOnWindows(msg);
		} else {
			Sayer.sayOnLinux(msg);
		}
	}

	@SneakyThrows
	public static Pare<Integer, List<String>> exec_filetmp(String cmdKey, String data, Path workDir, boolean throwIfHasError) {
		final Pare[] rslt = new Pare[1];
		new TmpFileOperation() {
			@Override
			public void doOperationImpl(Path executableFile) {
				executableFile = executableFile.toAbsolutePath();
				RW.write(executableFile, data);
				rslt[0] = exec_file(cmdKey, executableFile, workDir == null ? executableFile.getParent() : workDir, throwIfHasError);
			}
		}.doOperation();
		return rslt[0];
	}

//	@SneakyThrows
//	public static Pare<Integer, List<String>> exec_filetmp(String cmdKey, String data, boolean throwIfHasError) {
//		AtomicReference ref = new AtomicReference();
//		new TmpFolderOperation() {
//			@Override
//			public void doOperationImpl(Path tmpWorkDir) {
//				ref.set(exec_filetmp(cmdKey, data, tmpWorkDir, throwIfHasError));
//			}
//		}.doOperation();
//		return (Pare<Integer, List<String>>) ref.get();
//	}

	@SneakyThrows
	public static Pare<Integer, List<String>> exec_file(String cmdKey, Path executableFile, Path workDir, boolean throwIfHasError) {
		ProcessBuilder processBuilder = new ProcessBuilder(cmdKey, executableFile.toString());
		processBuilder.redirectErrorStream(true);
//		processBuilder.inheritIO();
		if (workDir != null) {
			processBuilder.directory(workDir.toFile());
		}
		Process process = processBuilder.start();
		List<String> results = RW.readLines(process.getInputStream());
		int exitCode = process.waitFor();
		String msg = X.fl("exec_file/{} >> {}", cmdKey, executableFile);
		if (L.isInfoEnabled()) {
			if (L.isDebugEnabled()) {
				L.debug(msg + " :::" + exitCode + "\n" + JOIN.allByNL(results));
			} else {
				L.info(msg + " :::" + exitCode + "*" + X.sizeOf(results));
			}
		}
		if (throwIfHasError && exitCode != 0) {
//			throw  new CleanMessageRuntimeException(JOIN.allByNL(results));
			throw new ExecRuntimeException(new CleanMessageRuntimeException(JOIN.allByNL(results)), msg);
		}
		return Pare.of(exitCode, results);
	}

	public static TimerTask RUN_TIMER(int beforeDelay, int period, TimerTask runnable) {
		new Timer().schedule(runnable, beforeDelay, period);
		return runnable;
	}


	public static void setClipboard(String writeMe, boolean... required) {
		try {
			Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable tText = new StringSelection(writeMe);
			clip.setContents(tText, null);
		} catch (Throwable ex) {
			if (ARG.isDefEqTrue(required)) {
				X.throwException(ex);
			}
		}
	}

	//	public static List<Thread> getThreads() {
//		List<Thread> all = Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().equals(threadName)).collect(Collectors.toList());
//		return X.notEmpty(all) ? all : ARG.toDefThrowMsg(() -> X.f("Thread '%s' not found", threadName), defRq);
//	}
	public static List<Thread> getThreads(String threadName, List<Thread>... defRq) {
		List<Thread> all = Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().equals(threadName)).collect(Collectors.toList());
		return X.notEmpty(all) ? all : ARG.toDefThrowMsg(() -> X.f("Thread '%s' not found", threadName), defRq);
	}

	public static boolean isThreadActive(String threadName) {
		Thread.State threadState = Sys.getThreadState(threadName, null);
		return threadState != null && threadState != Thread.State.TERMINATED;
	}


	public static Thread.State getThreadState(String threadName, Thread.State... defRq) {
		final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 10000);
		for (ThreadInfo threadInfo : threadInfos) {
			if (!threadName.equals(threadInfo.getThreadName())) {
				continue;
			}
			final Thread.State state = threadInfo.getThreadState();
			return state;
		}
		return ARG.toDefThrowMsg(() -> X.f("Thread '%s' not found", threadName), defRq);
	}

	public static Pare<Integer, List<String>> exePython3(String nodeData) {
		return exec_filetmp("python3", nodeData, null, false);
	}


	static class Sayer {

		public static void sayOnLinux(String message) {
			try {
				Runtime.getRuntime().exec("spd-say " + message.replaceAll("\\s+", "-"));
			} catch (IOException e) {
				p(e.getMessage());
			}
		}

		public static void sayOnWindows(String message) {
			// if WINDOWS
			String result = "";
			try {
				// File file = File.createTempFile("voicePC", ".vbs");
				File file = new File("voicePC.vbs");
				file.deleteOnExit();
				FileWriter fw = new FileWriter(file);

				String vbs = "CreateObject(\"SAPI.SpVoice\").Speak\"" + message + "\"";

				fw.write(vbs);
				fw.close();
				Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = input.readLine()) != null) {
					result += line;
				}
				input.close();

			} catch (Exception e) {
				p(e.getMessage());
			}
		}
	}

	public static Subject subject() {
		return Subject.getSubject(AccessController.getContext());
	}

}