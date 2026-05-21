let amapLoadPromise = null;

export function loadAmap() {
  if (window.AMap) {
    // #region debug-point A:existing-amap
    fetch("http://127.0.0.1:7777/event",{method:"POST",body:JSON.stringify({sessionId:"map-request-error",runId:"pre",hypothesisId:"A",location:"web/src/utils/amapLoader.js:5",msg:"[DEBUG] window.AMap already exists",data:{hasAMap:true},ts:Date.now()})}).catch(()=>{});
    // #endregion
    return Promise.resolve(window.AMap);
  }
  if (amapLoadPromise) {
    // #region debug-point A:reuse-promise
    fetch("http://127.0.0.1:7777/event",{method:"POST",body:JSON.stringify({sessionId:"map-request-error",runId:"pre",hypothesisId:"A",location:"web/src/utils/amapLoader.js:10",msg:"[DEBUG] reuse amap load promise",data:{hasPendingPromise:true},ts:Date.now()})}).catch(()=>{});
    // #endregion
    return amapLoadPromise;
  }

  const key = import.meta.env.VITE_AMAP_KEY;
  const securityJsCode = import.meta.env.VITE_AMAP_SECURITY_JS_CODE;
  // #region debug-point B:env-check
  fetch("http://127.0.0.1:7777/event",{method:"POST",body:JSON.stringify({sessionId:"map-request-error",runId:"pre",hypothesisId:"B",location:"web/src/utils/amapLoader.js:16",msg:"[DEBUG] amap env check",data:{hasKey:!!key,keyPrefix:key?String(key).slice(0,6):"",hasSecurityJsCode:!!securityJsCode,securityPrefix:securityJsCode?String(securityJsCode).slice(0,6):""},ts:Date.now()})}).catch(()=>{});
  // #endregion
  if (!key) {
    // #region debug-point B:missing-key
    fetch("http://127.0.0.1:7777/event",{method:"POST",body:JSON.stringify({sessionId:"map-request-error",runId:"pre",hypothesisId:"B",location:"web/src/utils/amapLoader.js:19",msg:"[DEBUG] missing VITE_AMAP_KEY",data:{hasKey:false},ts:Date.now()})}).catch(()=>{});
    // #endregion
    return Promise.reject(new Error("缺少 VITE_AMAP_KEY，请先在 web/.env.local 中配置高德 Key"));
  }
  if (!securityJsCode) {
    // #region debug-point B:missing-security
    fetch("http://127.0.0.1:7777/event",{method:"POST",body:JSON.stringify({sessionId:"map-request-error",runId:"pre",hypothesisId:"B",location:"web/src/utils/amapLoader.js:24",msg:"[DEBUG] missing VITE_AMAP_SECURITY_JS_CODE",data:{hasSecurityJsCode:false},ts:Date.now()})}).catch(()=>{});
    // #endregion
    return Promise.reject(new Error("缺少 VITE_AMAP_SECURITY_JS_CODE，请先在 web/.env.local 中配置高德安全密钥"));
  }

  amapLoadPromise = new Promise((resolve, reject) => {
    const callbackName = "__amap_init_cb__";

    // 高德安全密钥配置必须在 SDK 脚本加载前注入全局变量。
    window._AMapSecurityConfig = {
      securityJsCode
    };

    window[callbackName] = () => {
      // #region debug-point C:sdk-loaded
      fetch("http://127.0.0.1:7777/event",{method:"POST",body:JSON.stringify({sessionId:"map-request-error",runId:"pre",hypothesisId:"C",location:"web/src/utils/amapLoader.js:38",msg:"[DEBUG] amap sdk callback invoked",data:{callbackName,hasAMap:!!window.AMap},ts:Date.now()})}).catch(()=>{});
      // #endregion
      resolve(window.AMap);
      delete window[callbackName];
    };

    const script = document.createElement("script");
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}&plugin=AMap.Scale,AMap.ToolBar,AMap.Driving,AMap.Geolocation,AMap.Geocoder&callback=${callbackName}`;
    // #region debug-point C:script-created
    fetch("http://127.0.0.1:7777/event",{method:"POST",body:JSON.stringify({sessionId:"map-request-error",runId:"pre",hypothesisId:"C",location:"web/src/utils/amapLoader.js:45",msg:"[DEBUG] amap sdk script appended",data:{src:script.src},ts:Date.now()})}).catch(()=>{});
    // #endregion
    script.async = true;
    script.onerror = () => {
      // #region debug-point D:script-error
      fetch("http://127.0.0.1:7777/event",{method:"POST",body:JSON.stringify({sessionId:"map-request-error",runId:"pre",hypothesisId:"D",location:"web/src/utils/amapLoader.js:50",msg:"[DEBUG] amap sdk script load failed",data:{src:script.src},ts:Date.now()})}).catch(()=>{});
      // #endregion
      reject(new Error("高德地图 SDK 加载失败，请检查网络或 Key 配置"));
      delete window[callbackName];
      amapLoadPromise = null;
    };
    document.head.appendChild(script);
  });

  return amapLoadPromise;
}
