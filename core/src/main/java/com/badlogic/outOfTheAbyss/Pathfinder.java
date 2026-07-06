package com.badlogic.outOfTheAbyss;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class Pathfinder {

    private static class Node {
        int x, y;
        float gCost, hCost;
        Node parent;

        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }

        float fCost() {
            return gCost + hCost;
        }
    }

    public static Array<Vector2> encontrarCaminho(Vector2 inicio, Vector2 destino, Array<Rectangle> hitboxesMapa,
                                                  int larguraMapa, int alturaMapa, int raioSeguranca) {

        if (larguraMapa <= 0 || alturaMapa <= 0) return null;

        int offsetY = alturaMapa;

        boolean[][] bloqueado = construirGradeBloqueada(hitboxesMapa, larguraMapa, alturaMapa, offsetY, raioSeguranca);

        int startX = (int) inicio.x;
        int startY = (int) inicio.y + offsetY;
        int endX = (int) destino.x;
        int endY = (int) destino.y + offsetY;

        if (startX < 0 || startY < 0 || startX >= larguraMapa || startY >= alturaMapa) return null;
        if (endX < 0 || endY < 0 || endX >= larguraMapa || endY >= alturaMapa) return null;

        if (bloqueado[endX][endY]) {
            int[] alternativa = encontrarCelulaLivreProxima(bloqueado, endX, endY, larguraMapa, alturaMapa);
            if (alternativa == null) return null;
            endX = alternativa[0];
            endY = alternativa[1];
        }

        Node startNode = new Node(startX, startY);

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::fCost));
        Set<Long> closedSet = new HashSet<>();
        Map<Long, Node> allNodes = new HashMap<>();

        startNode.hCost = heuristica(startX, startY, endX, endY);
        openSet.add(startNode);
        allNodes.put(chave(startX, startY), startNode);

        int[][] direcoes = {
            {0, 1}, {0, -1}, {1, 0}, {-1, 0},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };

        int maxIteracoes = 800;
        int iteracoes = 0;

        while (!openSet.isEmpty() && iteracoes++ < maxIteracoes) {
            Node current = openSet.poll();

            if (current.x == endX && current.y == endY) {
                return reconstruirCaminho(current, offsetY);
            }

            closedSet.add(chave(current.x, current.y));

            for (int[] dir : direcoes) {
                int nx = current.x + dir[0];
                int ny = current.y + dir[1];

                if (nx < 0 || ny < 0 || nx >= larguraMapa || ny >= alturaMapa) continue;
                if (bloqueado[nx][ny]) continue;
                if (closedSet.contains(chave(nx, ny))) continue;

                if (dir[0] != 0 && dir[1] != 0) {
                    boolean bloqueiaX = bloqueado[current.x + dir[0]][current.y];
                    boolean bloqueiaY = bloqueado[current.x][current.y + dir[1]];
                    if (bloqueiaX || bloqueiaY) continue;
                }

                float custoMovimento = (dir[0] != 0 && dir[1] != 0) ? 1.4f : 1f;
                float gTentativo = current.gCost + custoMovimento;

                Node vizinho = allNodes.get(chave(nx, ny));
                if (vizinho == null) {
                    vizinho = new Node(nx, ny);
                    vizinho.gCost = Float.MAX_VALUE;
                    allNodes.put(chave(nx, ny), vizinho);
                }

                if (gTentativo < vizinho.gCost) {
                    vizinho.parent = current;
                    vizinho.gCost = gTentativo;
                    vizinho.hCost = heuristica(nx, ny, endX, endY);

                    openSet.remove(vizinho);
                    openSet.add(vizinho);
                }
            }
        }

        return null;
    }

    private static boolean[][] construirGradeBloqueada(Array<Rectangle> hitboxesMapa, int largura, int altura,
                                                       int offsetY, int raioSeguranca) {
        boolean[][] gradeBase = new boolean[largura][altura];
        for (Rectangle r : hitboxesMapa) {
            int gx = (int) r.x;
            int gy = (int) r.y + offsetY;
            if (gx >= 0 && gy >= 0 && gx < largura && gy < altura) {
                gradeBase[gx][gy] = true;
            }
        }

        if (raioSeguranca <= 0) return gradeBase;

        boolean[][] dilatada = new boolean[largura][altura];
        for (int x = 0; x < largura; x++) {
            for (int y = 0; y < altura; y++) {
                if (gradeBase[x][y]) {
                    for (int dx = -raioSeguranca; dx <= raioSeguranca; dx++) {
                        for (int dy = -raioSeguranca; dy <= raioSeguranca; dy++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx >= 0 && ny >= 0 && nx < largura && ny < altura) {
                                dilatada[nx][ny] = true;
                            }
                        }
                    }
                }
            }
        }
        return dilatada;
    }

    private static int[] encontrarCelulaLivreProxima(boolean[][] bloqueado, int x, int y, int largura, int altura) {
        for (int raio = 1; raio <= Math.max(largura, altura); raio++) {
            for (int dx = -raio; dx <= raio; dx++) {
                for (int dy = -raio; dy <= raio; dy++) {
                    int nx = x + dx, ny = y + dy;
                    if (nx >= 0 && ny >= 0 && nx < largura && ny < altura && !bloqueado[nx][ny]) {
                        return new int[]{nx, ny};
                    }
                }
            }
        }
        return null;
    }

    private static float heuristica(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private static long chave(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private static Array<Vector2> reconstruirCaminho(Node node, int offsetY) {
        Array<Vector2> caminho = new Array<>();
        while (node != null) {
            caminho.insert(0, new Vector2(node.x + 0.5f, (node.y - offsetY) + 0.5f));
            node = node.parent;
        }
        return caminho;
    }
}
