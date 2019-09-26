package com.uzduotis.lukauskis.marius;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.LinkedList;

public class main {
    public static void main(String[] args) {
        String currencyType = "";
        String currencyCode = "";
        String dateFrom = "";
        String dateTo = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String firstCurrencyXml = getRequest("http://www.lb.lt/webservices/fxrates/FxRates.asmx/" +
                "getFxRatesForCurrency?tp=eu&ccy=aud&dtFrom=2014-09-30&dtTo=");

        System.out.println("All inputs are optional.");
        try {
            System.out.println("Enter currency type(eu, lt) you can leave blank");
            currencyType = reader.readLine();

            System.out.println("Enter currency code(USD, GBP etc.) you can leave blank");
            currencyCode =reader.readLine();

            System.out.println("Enter date from(2018-01-01) you can leave blank");
            dateFrom = reader.readLine();

            System.out.println("Enter date to(2018-01-01) you can leave blank");
            dateTo = reader.readLine();

        } catch (IOException e) {
            e.printStackTrace();
        }
        String url = "http://www.lb.lt/webservices/fxrates/FxRates.asmx/getFxRatesForCurrency?tp="
                +currencyType+"&ccy="+currencyCode+"&dtFrom="+dateFrom+"&dtTo="+dateTo;

        String fullList = getRequest(url);

        if (fullList == "Can't react server") {
            System.out.println(fullList);
            return;
        }
        LinkedList<Currency> firstCurrency = xmlToObject(firstCurrencyXml);
        LinkedList<Currency> currencieList = xmlToObject(fullList);
        if (currencieList.size() == 0) {
            return;
        }
        printList(currencieList, firstCurrency);
    }

    private static void printList(LinkedList<Currency> currencieList, LinkedList<Currency> firstCurrency) {

        if (currencieList.size() == 1) {
            double currencyDiff = currencieList.get(0).getCurrency() - firstCurrency.get(0).getCurrency();
            double roundCurrencyDif = Double.parseDouble(String.format("%.5f", currencyDiff));
            System.out.println(currencieList.get(0).getCode() + ": " + currencieList.get(0).getCurrency()
                    + ". Ratio: " + roundCurrencyDif);
            return;
        }

        if (currencieList.get(0).getCode().equals(currencieList.get(1).getCode())) {
            double currencyDiff = currencieList.get(currencieList.size() - 1).getCurrency()
                    - currencieList.get(0).getCurrency();
            double roundCurrencyDif = Double.parseDouble(String.format("%.5f", currencyDiff));
            System.out.println(currencieList.get(0).getCode() + ": " + currencieList.get(0).getCurrency()
                    + ". Ratio: " + roundCurrencyDif);
            return;
        }
        if (!currencieList.get(0).getCode().equals(currencieList.get(1).getCode())) {
            for(int i = 0;i<currencieList.size();i++){
                System.out.println(currencieList.get(i).getDate()+" "+currencieList.get(i).getCode()+": "+currencieList.get(i).getCurrency());
            }
        }
    }

    private static LinkedList<Currency> xmlToObject(String xmlString) {
        LinkedList<Currency> currencyList = new LinkedList<Currency>();
        try {
            DocumentBuilderFactory dbf =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xmlString));

            Document doc = db.parse(is);

            NodeList nodes = doc.getElementsByTagName("FxRate");
            NodeList nodesErrors = doc.getElementsByTagName("OprlErr");
            if (nodesErrors.getLength() > 0) {
                Element element = (Element) nodesErrors.item(0);
                NodeList descNode = element.getElementsByTagName("Desc");
                Element errorDesc = (Element) descNode.item(0);
                System.out.println(getCharacterDataFromElement(errorDesc));
                return currencyList;
            }


            for (int i = 0; i < nodes.getLength(); i++) {
                Currency currencyObject = new Currency();
                Element element = (Element) nodes.item(i);
                NodeList DtNode = element.getElementsByTagName("Dt");
                Element DtAmtNodeElement = (Element) DtNode.item(0);
                String date = getCharacterDataFromElement(DtAmtNodeElement);

                NodeList CcyAmtNode = element.getElementsByTagName("CcyAmt");
                Element CcyAmtNodeElement = (Element) CcyAmtNode.item(1);

                NodeList codeNode = CcyAmtNodeElement.getElementsByTagName("Ccy");
                NodeList currencyNode = CcyAmtNodeElement.getElementsByTagName("Amt");

                Element codeLine = (Element) codeNode.item(0);
                Element currencyLine = (Element) currencyNode.item(0);

                String code = getCharacterDataFromElement(codeLine);
                String currency = getCharacterDataFromElement(currencyLine);

                if (code != "?" && currency != "?" && date != "?") {
                    currencyObject.setCode(code);
                    currencyObject.setCurrency(Double.valueOf(currency));
                    LocalDate localDate = LocalDate.parse(date);
                    currencyObject.setDate(localDate);
                    currencyList.add(currencyObject);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return currencyList;
    }

    public static String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "?";
    }

    private static String getRequest(String urlString) {
        URL url = null;
        String resp = "";
        try {
            url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            if (status < 200 && status > 299) {
                return "Can't react server";
            }
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            resp = content.toString();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resp;
    }

}
