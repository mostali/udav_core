package mpe.wthttp;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import mpc.fs.UF;
import mpc.fs.UFS;
import mpc.fs.ext.EXT;
import mpc.fs.path.IPath;
import mpc.map.MAP;
import mpc.rfl.RFL;
import mpc.types.ruprops.URuProps;
import mpe.rt.Thread0;
import mpe.wthttp.core.INode;
import mpf.zcall.ZType;
import mpu.IT;
import mpu.X;
import mpu.core.ARG;
import mpu.core.ARR;
import mpu.core.RW;
import mpu.pare.Pare;
import mpu.str.SPLIT;
import mpu.str.STR;
import mpu.str.TKN;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JarCallMsg extends CallMsg {

	public static final String KEY = "jartask";

	private @Setter String className;

	private @Setter String classMethodName;

	public final Map<String, Object> headersParams;
	public final @Getter Multimap context;

	public static boolean isValidKeyFirstLine(String data) {
		return STR.startsWith(data, KEY + ":");
	}

	public static void main(String[] args) {

	}

	public static JarCallMsg ofNode(INode nodeData) {
//		NodeDir node = (NodeDir) nodeData.toNode();
//		final JarCallMsg jarCallMsg = (JarCallMsg) JarCallMsg.of(nodeData.toNodeData(), true).setFromSrc(node.nodeId()).fromNode(nodeData);
		final JarCallMsg jarCallMsg = (JarCallMsg) of(nodeData.toNodeData(), true).fromNode(nodeData);
		Path jarPath = jarCallMsg.getJarPath(nodeData.toPath(), null);
		if (jarPath == null) {
			jarCallMsg.setJarPathFromNode(null);
		}
		return jarCallMsg;
	}

	public JarCallMsg(INode iNode) {
		this(iNode.toNodeData(), true);
	}


	//
	//

	public JarCallMsg(String fullMsg, boolean... lazyValid) {
		super(fullMsg, true);

		switch (state) {
			case EMPTY:
				headersParams = null;
				context = null;
				addError("Empty msg");
				return;

			case LINE:
				headersParams = new HashMap<>();
				context = LinkedListMultimap.create();
				break;

			default:
			case BODY:
				headersParams = getHeaders_MAP();
				context = getBody_MMAP();
				break;
		}


		String classWithMethodStr = TKN.lastGreedy(line0, KEY + ":", null);
		if (classWithMethodStr == null) {
			addError("Set KEY & class with method use pattern [%s::package.classname#methhod] in line '%s'", KEY, line0);
			return;
		}

		String[] classWithMethod = TKN.two(classWithMethodStr, "#", null);

		if (classWithMethod == null) {
			addError("Set class with method use pattern [package.classname#methhod] in line '%s'", line0);
			return;
		}

		this.className = classWithMethod[0];
		this.classMethodName = classWithMethod[1];

		if (ARG.isDefEqTrue(lazyValid)) {
			//ok, need lazy valid
		} else {
			doLazyValid(true);
		}

	}

	public JarCallMsg doLazyValid(boolean... silent) {
		if (X.empty(className)) {
			addErrorIfNotExists("Set CLASSNAME use pattern [package.classname#methhod] in line '%s'", line0);
		} else if (X.empty(classMethodName)) {
			addErrorIfNotExists("Set class METHOD name use pattern [package.classname#methhod] in line '%s'", line0);
		}
		if (ARG.isDefEqTrue(silent)) {
			throwIsErr();
		}
		return this;
	}

	//
	//


	@Override
	public Object call(boolean throwIfHasError) {
//		return super.call(throwIfHasError);
		return invokeJarMethod();
	}

	public Object invokeJarMethod() {
		return invokeJarMethod(this);
	}

	@Override
	public String type(Object... defRq) {
		return KEY;
	}

	@SneakyThrows
	public static Object invokeJarMethod(JarCallMsg jcm) {

		jcm.doLazyValid();

		ZType zType = ZType.of(jcm.getJarPath(), jcm.getClassName());

		ZType.ZMethod zMethod = zType.getMethodByName(jcm.getClassMethodName());

		if (zMethod != null) {
			return ZType.ZMethod.invokeCallMsg(jcm, zMethod);
		}

		return invokeCallMsgNative(jcm);
	}

	private static Object invokeCallMsgNative(JarCallMsg jcm) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException, MalformedURLException {
		String classMethodName = jcm.getClassMethodName();
		Object rsltInvoke;
		if (jcm.isMainOrInvokeLinesMethod()) {
			Map map = jcm.getContext().asMap();
			String[] args = URuProps.toLinesMultimapAsSeq(map);
//			String[] args = msg.getHeaders_SEQARGS()
			rsltInvoke = RFL.invokeJarSt_(jcm.getJarPath(), jcm.getClassName(), classMethodName, new Class[]{String[].class}, new Object[]{args});
		} else {
			rsltInvoke = RFL.invokeJarSt_(jcm.getJarPath(), jcm.getClassName(), classMethodName, new Class[]{Object.class, Map.class}, new Object[]{null, jcm.getContext().asMap()});
		}
		return rsltInvoke;
	}

	private static final String[] mainMetods = {"main", "invokeLines"};

	public boolean isMainOrInvokeLinesMethod() {
		return ARR.contains(mainMetods, getClassMethodName());
	}

	public Pare<Thread0, Object> invokeJarMethodAsyncAndWait(long joinMs, Object... defRq) {
		Thread0 objThread = new Thread0(getClass().getSimpleName() + "_Async", true) {
			@Override
			public void run() {
				set_result_object(invokeJarMethod());
			}
		};
		Object andWaitResult = objThread.getAndWaitResult(joinMs, defRq);
		objThread.throwIfHasErrors();
		return Pare.of(objThread, andWaitResult);
	}

	public Thread0 invokeJarMethodAsync() {
		return new Thread0(getClass().getSimpleName() + "_Async_" + getNode().toNodeId(), true) {
			@Override
			public void run() {
				set_result_object(invokeJarMethod());
			}
		};
	}


	public Integer getAsyncWaitMs(Integer... defRq) {
		return MAP.getAsInt(headersParams, "async.wait.ms", defRq);
	}

	public String getClassName(String... defRq) {
		return X.notEmpty(className) ? className : ARG.toDefThrowMsg(() -> X.f("Set className"), defRq);
	}

	public String getClassMethodName(String... defRq) {
		return X.notEmpty(classMethodName) ? classMethodName : ARG.toDefThrowMsg(() -> X.f("Set methodName"), defRq);
	}

	//
	//

	public JarCallMsg setDirParam(String path) {
		headersParams.put("dir", path);
		return this;
	}

	public Path getDir(Path... defRq) {
		return MAP.getAs(headersParams, "dir", Path.class, defRq);
	}

	public String[] getPackages(String... defRq) {
		return SPLIT.argsByComma(getHeaderParam("packages", defRq));
	}

	public String getHeaderParam(String key, String... defRq) {
		return MAP.getAs(headersParams, key, String.class, defRq);
	}

	//
	//

	public void setJarPath(Path path) {
		setDirParam(path.getParent().toString());
		setJarFilename(path.getFileName().toString());
	}

	public Path getJarPath(Path... defRq) {
		return getJarPath(null, defRq);
	}

	public Path getJarPath(Path parent, Path... defRq) {
		Path dir0 = getDir(null);
		if (dir0 == null) {
			return ARG.toDefThrowMsg(() -> X.f("set header param [dir] with optional param [jar.filename]"), defRq);
		}
		Path dir;
		if (parent != null && UF.isPathStartFromParent(dir0)) {
//			dir = UF.normParentCharacter(dir0, parent != null ? parent.getParent() : Env.RUN_LOCATION.toAbsolutePath());
			dir = UF.normParentCharacter(dir0, parent.getParent());
		} else {
			dir = UF.normHomeCharacter(dir0);
		}
		String jarFilename = getJarFilename(null);
		if (jarFilename != null) {
			Path jarFile = dir.resolve(jarFilename);
			return UFS.existFile(jarFile) ? jarFile : ARG.toDefThrowMsg(() -> X.f("File Jar '%s' not found from dir '%s'. Check param [dir] or optional param [jar.filename]", jarFile, dir), defRq);
		}
		List<Path> paths = IPath.of(dir).dLsEXT(EXT.JAR);
		if (X.empty(paths)) {
			return ARG.toDefThrowMsg(() -> X.f("File Jar not found from dir '%s'. May be need set optional param [jar.filename]?", dir), defRq);
		} else if (paths.size() > 1) {
//			return ARG.toDefThrowMsg(() -> X.f("Except only one jar file from dir '%s', but found '%s'", dir, STREAM.mapToList(paths, UF::fn)), defRq);
			return X.throwException(X.f("Except only one jar file from dir '%s'. May be need set optional param [jar.filename]?", dir));
		}
		return paths.get(0);
	}

	public String getJarFilename(String... defRq) {
		return MAP.getAs(headersParams, "jar.filename", String.class, defRq);
	}

	public JarCallMsg setJarFilename(String jarFilename) {
		headersParams.put("jar.filename", jarFilename);
		return this;
	}


	@Override
	public String toString() {
		return "JarCallMsg{" +
//				"msg='" + fullMsg + '\'' +
				", class='" + className + '\'' + ", method='" + classMethodName + '\'' + ", headers=" + headersParams + ", context=" + context + ", errs=" + X.sizeOf0(getErrors()) + '}';
	}


	public static JarCallMsg of(IPath file, boolean... lazyValid) {
		return (JarCallMsg) ofQk(file, lazyValid).throwIsErr();
	}

	public static JarCallMsg ofQk(Path file, boolean... lazyValid) {
		return ofQk(IPath.of(file), lazyValid);
	}

	public static JarCallMsg ofQk(IPath file, boolean... lazyValid) {
		return (JarCallMsg) of(file.fCat(), lazyValid).setFromSrc(file);
	}

	public static JarCallMsg of(Path file, boolean... lazyValid) {
		return of(RW.readContent(file), lazyValid);
	}

	public static JarCallMsg of(String msg, boolean... lazyValid) {
		return (JarCallMsg) ofQk(msg, lazyValid).throwIsErr();
	}

	public static JarCallMsg ofQk(String msg, boolean... lazyValid) {
		return new JarCallMsg(msg, lazyValid);
	}

	public static boolean isValid(String data) {
		return JarCallMsg.ofQk(data).isValid();
	}


	public boolean isSync() {
		return getAsyncWaitMs(-1) < 0;
	}

	public void setJarPathFromNode(IPath nodeNU) {
		IPath node = getNode();
		JarCallMsg jarCallMsg = this;
		List<Path> nodeJars = node.dLsEXT(EXT.JAR);
		if (X.empty(nodeJars)) {
			Path dir = jarCallMsg.getDir(null);
			if (dir != null) {
				if (dir.toString().startsWith("./")) {
					//only for relative paths
					Path checkDir = node.toPath().resolve(jarCallMsg.getDir());
					nodeJars = IPath.of(checkDir).dLsEXT(EXT.JAR);
				}
			}
		}
		IT.notEmpty(nodeJars, "Add jar to node, or set header param [dir] with optional param [jar.filename]");
//		IT.isLength(nodeJars, 1, "Except only one jar file, but found '%s'", AFCC.relativizeAppFile(nodeJars));
		jarCallMsg.setJarPath(nodeJars.get(0));
	}
}
