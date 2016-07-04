import groovy.util.slurpersupport.GPathResult

@Grab('net.sourceforge.nekohtml:nekohtml:1.9.16')

public class RuleProviderReportUtil {

    public static GPathResult loadHTML(String file) {
        def parser = new org.cyberneko.html.parsers.DOMParser()
        parser.parse(file);
        def documentW3cDom = parser.getDocument();
        def documentString = groovy.xml.XmlUtil.serialize(documentW3cDom.documentElement);
        def document = new XmlSlurper().parseText(documentString);
        return document;
    }

    public static List listFailedRules(String file) {
        return listAllRules(file).findAll{
            it.result != "success"
        };
    }



    public static Map<String, List<Map<String, String>>> findDifferences(String previousFile, String newFile) {
        List<Map<String, String>> previousRules = listAllRules(previousFile);
        List<Map<String, String>> newRules = listAllRules(newFile);

        // Find rules executed previously, but not in the new run
        def onlyInPrevious = previousRules.findAll {
            def rulesFromNew = newRules.findAll { newRule ->
                (
                        newRule["ruleID"] == it["ruleID"] ||
                        removeWithID(newRule["rule"]) == removeWithID(it["rule"])
                ) &&
                newRule.executed == it.executed
            }

            return rulesFromNew.isEmpty();
        };

        def onlyInNew = newRules.findAll {
            def rulesFromOld = previousRules.findAll { previousRule ->
                (
                        previousRule["ruleID"] == it["ruleID"] ||
                                removeWithID(previousRule["rule"]) == removeWithID(it["rule"])
                ) &&
                previousRule.executed == it.executed
            }

            return rulesFromOld.isEmpty();
        };

        // return the result
        return [
                onlyInPrevious: onlyInPrevious,
                onlyInNew: onlyInNew
        ]
    }

    private static String removeWithID(String rule) {
        return rule.replaceFirst("withId\\(.*?\\)", "");
    }

    public static List<Map<String, String>> listAllRules(String file) {
        def allRules = [];
        def document = loadHTML(file);
        document.'**'.grep { node ->
            node.name() == 'TABLE' &&
                    node.@class.toString().contains("table-striped") &&
                    node.TBODY.TR[0].TH[0].toString().equals("Rule-ID")
        }.each {
            String ruleProviderID = it.'..'.DIV.H3.toString();
            String phaseLine = it.'..'.DIV.toString();
            String phase = (phaseLine =~ /Phase: (.*)/)[0][1];

            it.TBODY.TR.list().drop(1).each {
                String ruleID = it.TD[0].toString().trim();
                String rule = it.TD[1].toString().trim();
                def statisticsElement = it.TD[2];
                String executed = it.TD[3].toString().trim();
                String result = it.TD[4].toString().trim();
                allRules.add([
                        ruleProvider: ruleProviderID,
                        ruleID      : ruleID,
                        rule      : rule,
                        phase       : phase,
                        executed    : executed,
                        result      : result,
                        statistics  : statisticsElement.toString()
                ])
                //println "Row: " + ruleID + ", Rule Provider: " + ruleProviderID + ", Phase: " + phase + " result: " + result + ", stats: " + statisticsElement.name();
            }
        }
        return allRules;
    }
}

