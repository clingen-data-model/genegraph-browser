(ns genegraph.browser
  (:require [reagent.core :as r]
            [reagent.dom.client :as rc]
            [re-frame.core :as re-frame]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]))

(enable-console-print!)

(re-frame/reg-event-db ::initialize-db
  (fn [db _]
    (if db
      db
      {:current-route nil})))

(defn home-page []
  [:div
   [:h2 "Welcome to frontend"]

   [:button
    {:type "button"
     :on-click #(rfe/push-state ::item {:id 3})}
    "Item 3"]

   [:button
    {:type "button"
     :on-click #(rfe/replace-state ::item {:id 4})}
    "Replace State Item 4"]])

(defn about-page []
  [:div
   [:h2 "About frontend"]
   [:ul
    [:li [:a {:href "http://google.com"} "external link"]]
    [:li [:a {:href (rfe/href ::foobar)} "Missing route"]]
    [:li [:a {:href (rfe/href ::item)} "Missing route params"]]]

   [:div
    {:content-editable true
     :suppressContentEditableWarning true}
    [:p "Link inside contentEditable element is ignored."]
    [:a {:href (rfe/href ::frontpage)} "Link"]]])

(defn item-page [match]
  (let [{:keys [path query]} (:parameters match)
        {:keys [id]} path]
    [:div
     [:h2 "Selected item " id]
     (if (:foo query)
       [:p "Optional foo query param: " (:foo query)])]))

(defn current-page []
  (let [current-route @(re-frame/subscribe [::current-route])]
   [:div
    [:ul
     [:li [:a {:href (rfe/href ::frontpage)} "Frontpage"]]
     [:li [:a {:href (rfe/href ::about)} "About"]]
     [:li [:a {:href (rfe/href ::item {:id 1})} "Item 1"]]
     [:li [:a {:href (rfe/href ::item {:id 2} {:foo "bar"})} "Item 2"]]]
    (if current-route
      (let [view (:view (:data current-route))]
        [view current-route]))
    [:pre (with-out-str (cljs.pprint/pprint current-route))]]))

(def routes
  [["/"
    {:name ::frontpage
     :view home-page}]

   ["/about"
    {:name ::about
     :view about-page}]

   ["/item/:id"
    {:name ::item
     :view item-page
     :parameters {:path {:id int?}}}]])

(defonce root (atom nil))

(defn main-panel []
  [:div "hello world! I am here five"])

(defn create-root! []
  (when-not @root
    (let [r (rc/create-root (.getElementById js/document "app"))]
      (reset! root r))))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (create-root!)
  (rc/render @root [current-page]))

(re-frame/reg-sub ::current-route
  (fn [db]
    (:current-route db)))

(re-frame/reg-event-fx
 ::push-state
 (fn [_ [_ & route]]
   {:push-state route}))

(re-frame/reg-event-db
 ::navigated
 (fn [db [_ new-match]]
   (let [old-match   (:current-route db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)]
     (assoc db :current-route (assoc new-match :controllers controllers)))))

(defn on-navigate [new-match]
  (when new-match
    (re-frame/dispatch [::navigated new-match])))

;; start is called by init and after code reloading finishes
(defn ^:dev/after-load start []
  (js/console.log "start")
  (rfe/start!
   (rf/router routes)
   on-navigate
   ;; set to false to enable HistoryAPI
   {:use-fragment false}))

(defn init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (js/console.log "init")
  (re-frame/clear-subscription-cache!)
  (re-frame/dispatch-sync [::initialize-db])
  (start)
  (mount-root))

;; this is called before any code is reloaded
(defn ^:dev/before-load stop []
  (js/console.log "stop"))
