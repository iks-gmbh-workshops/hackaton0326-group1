import { useEffect } from "react";
import { clsx } from "keycloakify/tools/clsx";
import { kcSanitize } from "keycloakify/lib/kcSanitize";
import type { TemplateProps } from "keycloakify/login/TemplateProps";
import { getKcClsx } from "keycloakify/login/lib/kcClsx";
import { useSetClassName } from "keycloakify/tools/useSetClassName";
import { useInitialize } from "keycloakify/login/Template.useInitialize";
import type { I18n } from "./i18n";
import type { KcContext } from "./KcContext";
import IKSLogo from "../assets/IKS_Logo.svg";
import Logo from "../assets/icon.png";
import "./main.css";

export default function Template(props: TemplateProps<KcContext, I18n>) {
  const {
    displayMessage = true,
    displayRequiredFields = false,
    headerNode,
    socialProvidersNode = null,
    documentTitle,
    bodyClassName,
    kcContext,
    i18n,
    doUseDefaultCss,
    classes,
    children
  } = props;

  const { kcClsx } = getKcClsx({ doUseDefaultCss, classes });

  const { msg, msgStr } = i18n;

  const { realm, auth, url, message, isAppInitiatedAction } = kcContext;

  useEffect(() => {
    document.title = documentTitle ?? msgStr("loginTitle", realm.displayName);
  }, []);

  useSetClassName({
    qualifiedName: "html",
    className: kcClsx("kcHtmlClass")
  });

  useSetClassName({
    qualifiedName: "body",
    className: bodyClassName ?? kcClsx("kcBodyClass")
  });

  const { isReadyToRender } = useInitialize({ kcContext, doUseDefaultCss });

  const isUpdatePasswordPage = kcContext.pageId === "login-update-password.ftl";

  if (!isReadyToRender) {
    return null;
  }

  return (
    <div className={"min-h-screen bg-white text-gray-700 flex flex-col grow overflow-x-hidden"}>
      <header className="bg-white shadow-xl h-full">
        <div className="px-4 py-2 flex justify-between items-center h-full">
          <div
            className="flex items-center ml-2 hover:cursor-pointer"
            onClick={() => {
              window.location.href = `${window.location.protocol}//${window.location.hostname}:3000/`;
            }}
          >
            <img src={Logo} width="80" alt="Logo HeuermannPlus" />
            <span className="kcHeader">HeuermannPlus</span>
          </div>
          <nav className="flex justify-between items-center h-full">
            <a href="https://www.iks-gmbh.com" target="_blank" rel="noreferrer">
              <img src={IKSLogo} width="140" alt="IKS Logo" />
            </a>
          </nav>
        </div>
      </header>

      <div className="content-center grow">
        <div className={kcClsx("kcFormCardClass")}>
          <header className={kcClsx("kcFormHeaderClass")}>
            {(() => {
              const node = !(auth !== undefined && auth.showUsername && !auth.showResetCredentials) ? (
                <h1 id="kc-page-title">{headerNode}</h1>
              ) : (
                <div id="kc-username" className={kcClsx("kcFormGroupClass")}>
                  <label id="kc-attempted-username">{auth.attemptedUsername}</label>
                  <a id="reset-login" href={url.loginRestartFlowUrl} aria-label={msgStr("restartLoginTooltip")}>
                    <div className="kc-login-tooltip">
                      <i className={kcClsx("kcResetFlowIcon")}></i>
                      <span className="kc-tooltip-text">{msg("restartLoginTooltip")}</span>
                    </div>
                  </a>
                </div>
              );

              if (displayRequiredFields) {
                return (
                  <div className={kcClsx("kcContentWrapperClass")}>
                    <div className={clsx(kcClsx("kcLabelWrapperClass"), "subtitle")}>
                      <span className="subtitle">
                        <span className="required">*</span>
                        {msg("requiredFields")}
                      </span>
                    </div>
                    <div className="col-md-10">{node}</div>
                  </div>
                );
              }

              return node;
            })()}
          </header>
          <div id="kc-content">
            <div id="kc-content-wrapper">
              {/* App-initiated actions should not see warning messages about the need to complete the action during login. */}
              {displayMessage && message !== undefined && (message.type !== "warning" || !isAppInitiatedAction) && (
                <div
                  className={clsx(
                    `full-width`,
                    `alert-${message.type}`,
                    kcClsx("kcAlertClass"),
                    `pf-m-${message?.type === "error" ? "danger" : message.type}`
                  )}
                >
                  <div className="pf-c-alert__icon">
                    {message.type === "success" && <span className={kcClsx("kcFeedbackSuccessIcon")}></span>}
                    {message.type === "warning" && <span className={kcClsx("kcFeedbackWarningIcon")}></span>}
                    {message.type === "error" && <span className={kcClsx("kcFeedbackErrorIcon")}></span>}
                    {message.type === "info" && <span className={kcClsx("kcFeedbackInfoIcon")}></span>}
                  </div>
                  <span
                    className={kcClsx("kcAlertTitleClass")}
                    dangerouslySetInnerHTML={{
                      __html: kcSanitize(message.summary)
                    }}
                  />
                </div>
              )}
              {/* Login Update Password page should always show the password policy. */}
              {isUpdatePasswordPage && (
                <div
                  className={clsx(
                    `full-width`,
                    `alert-info`,
                    kcClsx("kcAlertClass"),
                    `pf-m-info`,
                    `mt-05`
                  )}
                >
                  <div className="pf-c-alert__icon">
                    <span className={kcClsx("kcFeedbackInfoIcon")}></span>
                  </div>
                  {/* See also password policy in keycloaksetup/setup.sh */}
                  <span
                    className={kcClsx("kcAlertTitleClass")}
                    dangerouslySetInnerHTML={{
                      __html: kcSanitize(
                        'Es gelten die folgenden Regeln für Passwörter:'
                        + '<br><ul>'
                        + '<li>- mindestens 16 Zeichen lang</li>'
                        + '<li>- mindestens 1 Großbuchstabe</li>'
                        + '<li>- mindestens 1 Kleinbuchstabe</li>'
                        + '<li>- mindestens 1 Zahl</li>'
                        + '<li>- keines der letzten 5 Passwörter</li>'
                        + '<li>- ungleich dem Benutzernamen</li>'
                        + '<li>- ungleich der E-Mail</li>'
                        + '</ul>'
                      )
                    }}
                  />
                </div>
              )}
              {children}
              {auth !== undefined && auth.showTryAnotherWayLink && (
                <form id="kc-select-try-another-way-form" action={url.loginAction} method="post">
                  <div className={kcClsx("kcFormGroupClass")}>
                    <input type="hidden" name="tryAnotherWay" value="on" />
                    <a
                      href="#"
                      id="try-another-way"
                      onClick={() => {
                        document.forms["kc-select-try-another-way-form" as never].requestSubmit();
                        return false;
                      }}
                    >
                      {msg("doTryAnotherWay")}
                    </a>
                  </div>
                </form>
              )}
              {socialProvidersNode}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
