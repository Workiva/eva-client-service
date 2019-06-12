// Copyright 2018-2019 Workiva Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.workiva.eva.clientservice.edn;

public class Transactions {

  public static String defaultSchema =
      "[\n"
          + "{:db/id #db/id [:db.part/user]\n"
          + " :db/ident :book/title\n"
          + " :db/doc \"Title of a book\"\n"
          + " :db/valueType :db.type/string\n"
          + " :db/cardinality :db.cardinality/one\n"
          + " :db.install/_attribute :db.part/db}\n"
          + "\n"
          + "{:db/id #db/id [:db.part/user]\n"
          + " :db/ident :book/year_published\n"
          + " :db/doc \"Date book was published\"\n"
          + " :db/valueType :db.type/long\n"
          + " :db/cardinality :db.cardinality/one\n"
          + " :db.install/_attribute :db.part/db}\n"
          + "\n"
          + "{:db/id #db/id [:db.part/user]\n"
          + " :db/ident :book/author\n"
          + " :db/doc \"Author of a book\"\n"
          + " :db/valueType :db.type/ref\n"
          + " :db/cardinality :db.cardinality/one\n"
          + " :db.install/_attribute :db.part/db}\n"
          + "\n"
          + "{:db/id #db/id [:db.part/user]\n"
          + " :db/ident :author/name\n"
          + " :db/doc \"Name of author\"\n"
          + " :db/valueType :db.type/string\n"
          + " :db/cardinality :db.cardinality/one\n"
          + " :db.install/_attribute :db.part/db}\n"
          + " ]";

  public static String defaultSchemaUniqueTitles =
      "[\n"
          + "{:db/id #db/id [:db.part/user]\n"
          + " :db/ident :book/title\n"
          + " :db/doc \"Title of a book\"\n"
          + " :db/valueType :db.type/string\n"
          + " :db/cardinality :db.cardinality/one\n"
          + " :db.install/_attribute :db.part/db\n"
          + " :db/unique :db.unique/identity}\n"
          + "\n"
          + "{:db/id #db/id [:db.part/user]\n"
          + " :db/ident :book/year_published\n"
          + " :db/doc \"Date book was published\"\n"
          + " :db/valueType :db.type/long\n"
          + " :db/cardinality :db.cardinality/one\n"
          + " :db.install/_attribute :db.part/db}\n"
          + "\n"
          + "{:db/id #db/id [:db.part/user]\n"
          + " :db/ident :book/author\n"
          + " :db/doc \"Author of a book\"\n"
          + " :db/valueType :db.type/ref\n"
          + " :db/cardinality :db.cardinality/one\n"
          + " :db.install/_attribute :db.part/db}\n"
          + "\n"
          + "{:db/id #db/id [:db.part/user]\n"
          + " :db/ident :author/name\n"
          + " :db/doc \"Name of author\"\n"
          + " :db/valueType :db.type/string\n"
          + " :db/cardinality :db.cardinality/one\n"
          + " :db.install/_attribute :db.part/db}\n"
          + " ]";

  public static String singleBook =
      "[\n"
          + "\t[:db/add #db/id [:db.part/user] :book/title \"First Book\"]\n"
          + "\t[:db/add #db/id [:db.part/tx] :author/name \"Billy Baroo\"]\n"
          + "]";

  public static String exInfoTxFunction =
      ""
          + "[{:db/id #db/id [:db.part/user -1]\n"
          + "  :db/ident :throw-ex-info\n"
          + "  :db/doc \"Throws an ExceptionInfo\"\n"
          + "  :db/fn #db/fn\n"
          + "         {:lang \"clojure\"\n"
          + "          :params [db]\n"
          + "          :code (throw \n"
          + "                  (ex-info \"The ice cream has melted!\" \n"
          + "                    {:causes              #{:fridge-door-open :dangerously-high-temperature} \n"
          + "                     :current-temperature {:value 25 :unit :celsius}})\n"
          + "         )}}\n"
          + "   ]";

  public static String callexInfoTxFunction = "[[:throw-ex-info]]";

  public static String generalExceptionTxFunction =
      ""
          + "[{:db/id #db/id [:db.part/user -1]\n"
          + "  :db/ident :throw-ex\n"
          + "  :db/doc \"Throws a general exception\"\n"
          + "  :db/fn #db/fn\n"
          + "         {:lang \"clojure\"\n"
          + "          :params [db]\n"
          + "          :code  (throw\n"
          + "                   (IllegalStateException.\n"
          + "                   (str \"RIP\")))}}\n"
          + "   ]";

  public static String callGeneralExceptionTxFunction = "[[:throw-ex]]";

  public static String generalNestedExceptionTxFunction =
      ""
          + "[{:db/id #db/id [:db.part/user -1]\n"
          + "  :db/ident :throw-ex-nested\n"
          + "  :db/doc \"wewt\"\n"
          + "  :db/fn #db/fn\n"
          + "         {:lang \"clojure\"\n"
          + "          :params [db]\n"
          + "          :code  (throw\n"
          + "                   (IllegalStateException.\n"
          + "                     (str \"RIP\") (RuntimeException. "
          + "                                     (str \"hello\")))"
          + "                 )}}\n"
          + "  ]";

  public static String callGeneralNestedExceptionTxFunction = "[[:throw-ex-nested]]";
}
