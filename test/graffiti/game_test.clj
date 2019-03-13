(ns graffiti.game-test
  (:require [graffiti.core :as g]
            [graffiti.specs :as specs]
            [graffiti.db :as db]
            [clojure.test :as t]))

;; resolvers

(g/defresolver game-resolver
  [ctx {:game/keys [id]}]
  {:input  #{:game/id}
   :output [:game/id :game/name {:game/designers [:designer/id]}]}
  (db/get-game id))

(g/defresolver designer-resolver
  [ctx {:designer/keys [id]}]
  {:input  #{:designer/id}
   :output [:designer/id :designer/name :designer/games]}
  (db/get-designer id))

;; setup

(def ^:const options
  {:lacinia/objects
   {:Game     specs/game
    :Designer specs/designer}

   :lacinia/queries
   {:game {:type  :Game
           :input #{:game/id}}}

   :pathom/resolvers
   [game-resolver designer-resolver]})

(def mesh (g/compile options))

;; query

(t/deftest graphql-query
  (t/is
    (= (g/graphql mesh "{ game(id: \"1234\") { id name designers { name games { name }}}}")
       {:data {:game {:id        "1234"
                      :name      "Uncharted"
                      :designers [{:name  "John"
                                   :games [{:name "Uncharted"}]}]}}})))

(t/deftest eql-query
  (t/is
    (= (g/eql mesh [{[:game/id "1234"]
                     [:game/id
                      :game/name {:game/designers [:designer/id
                                                   :designer/name {:designer/games [:game/name]}]}]}])
       {[:game/id "1234"] #:game{:id        "1234"
                                 :name      "Uncharted"
                                 :designers [#:designer{:id    "4567"
                                                        :name  "John"
                                                        :games [#:game{:name "Uncharted"}]}]}})))
