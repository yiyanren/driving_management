let amapLoadPromise = null;

export function loadAmap() {
  if (window.AMap) {
    return Promise.resolve(window.AMap);
  }
  if (amapLoadPromise) {
    return amapLoadPromise;
  }

  const key = import.meta.env.VITE_AMAP_KEY;
  const securityJsCode = import.meta.env.VITE_AMAP_SECURITY_JS_CODE;
  if (!key) {
    return Promise.reject(new Error("缺少 VITE_AMAP_KEY，请先在 web/.env.local 中配置高德 Key"));
  }
  if (!securityJsCode) {
    return Promise.reject(new Error("缺少 VITE_AMAP_SECURITY_JS_CODE，请先在 web/.env.local 中配置高德安全密钥"));
  }

  amapLoadPromise = new Promise((resolve, reject) => {
    const callbackName = "__amap_init_cb__";

    // 高德安全密钥配置必须在 SDK 脚本加载前注入全局变量。
    window._AMapSecurityConfig = {
      securityJsCode
    };

    window[callbackName] = () => {
      resolve(window.AMap);
      delete window[callbackName];
    };

    const script = document.createElement("script");
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}&plugin=AMap.Scale,AMap.ToolBar,AMap.Driving,AMap.Geolocation&callback=${callbackName}`;
    script.async = true;
    script.onerror = () => {
      reject(new Error("高德地图 SDK 加载失败，请检查网络或 Key 配置"));
      delete window[callbackName];
      amapLoadPromise = null;
    };
    document.head.appendChild(script);
  });

  return amapLoadPromise;
}
